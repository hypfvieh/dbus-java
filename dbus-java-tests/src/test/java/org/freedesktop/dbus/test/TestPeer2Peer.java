package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.freedesktop.dbus.connections.impl.DirectConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.test.helper.P2pTestServer;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestPeer2Peer extends AbstractBaseTest {

    private static final String CONNECTION_ADDRESS = TransportBuilder.createDynamicSession(TransportBuilder.getRegisteredBusTypes().getFirst(), false);

    private volatile boolean finished = false;

    @Test
    public void testP2p() throws InterruptedException {
        P2pServer p2pServer = new P2pServer();
        p2pServer.start();

        try (DirectConnection dc = DirectConnectionBuilder.forAddress(CONNECTION_ADDRESS).build()) {
            LoggerFactory.getLogger(getClass()).info("Client: Connected: {}", dc);

            SampleRemoteInterface tri = (SampleRemoteInterface) dc.getRemoteObject("/Test");

            logger.debug("{}", tri.getName());
            logger.debug("{}", tri.testfloat(new float[] {
                    17.093f, -23f, 0.0f, 31.42f
            }));

            try {
                tri.throwme();
            } catch (SampleException _ex) {
                logger.debug("Caught exception: {}", _ex.getMessage());
            }

            Peer peer = dc.getRemoteObject("/Test", Peer.class);
            peer.Ping();

            Introspectable intro = dc.getRemoteObject("/Test", Introspectable.class);

            String introspect = intro.Introspect();
            assertTrue(introspect.startsWith("<!DOCTYPE"));

            dc.disconnect();
            LoggerFactory.getLogger(getClass()).info("Client: Disconnected");
            finished = true;
        } catch (DBusException _ex) {
            fail("Exception in client", _ex);
        }
    }

    private final class P2pServer extends Thread {

        P2pServer() {
            setName("P2pServerThread");
        }

        @Override
        public void run() {
            try (DirectConnection dc = DirectConnectionBuilder.forAddress(CONNECTION_ADDRESS + ",listen=true").build()) {
                dc.exportObject("/Test", new P2pTestServer());
                LoggerFactory.getLogger(getClass()).info("Server: Export created");

                LoggerFactory.getLogger(getClass()).info("Server: Listening");
                dc.listen();

                while (!finished) {
                    sleep(500L);
                }
            } catch (IOException | DBusException | InterruptedException  _ex) {
                fail("Exception in server", _ex);
            }
        }

    }
}
