package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.test.helper.P2pTestServer;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class TestPeer2Peer {

    private volatile boolean finished = false;

    private static final String CONNECTION_ADDRESS = TransportFactory.createDynamicSession(TransportFactory.getRegisteredBusTypes().get(0), false);

    @Test
    public void testP2p() throws InterruptedException {
        P2pServer p2pServer = new P2pServer();
        p2pServer.start();
        Thread.sleep(1000L);

        try (DirectConnection dc = new DirectConnection(CONNECTION_ADDRESS)) {
            Thread.sleep(500L);
            LoggerFactory.getLogger(getClass()).info("Client: Connected");
            SampleRemoteInterface tri = (SampleRemoteInterface) dc.getRemoteObject("/Test");
            System.out.println(tri.getName());
            System.out.println(tri.testfloat(new float[] {
                    17.093f, -23f, 0.0f, 31.42f
            }));

            try {
                tri.throwme();
            } catch (SampleException ex) {
            }

            Peer peer = dc.getRemoteObject("/Test", Peer.class);
            peer.Ping();

            Introspectable intro = dc.getRemoteObject("/Test", Introspectable.class);

            String introspect = intro.Introspect();
            assertTrue(introspect.startsWith("<!DOCTYPE"));

            dc.disconnect();
            LoggerFactory.getLogger(getClass()).info("Client: Disconnected");
            finished = true;
        } catch (IOException | DBusException _ex) {
            _ex.printStackTrace();
            fail("Exception in client");
        }
    }


    private class P2pServer extends Thread {

        @Override
        public void run() {
            try (DirectConnection dc = new DirectConnection(CONNECTION_ADDRESS + ",listen=true")) {
                dc.exportObject("/Test", new P2pTestServer());
                LoggerFactory.getLogger(getClass()).info("Server: Export created");

                LoggerFactory.getLogger(getClass()).info("Server: Listening");
                dc.listen();

                while (!finished) {
                    Thread.sleep(500L);
                }
            } catch (IOException | DBusException | InterruptedException  _ex) {
                _ex.printStackTrace();
                fail("Exception in server");
            }
        }

    }
}
