package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.test.helper.cross.CrossTestClient;
import org.freedesktop.dbus.test.helper.cross.CrossTestServer;
import org.freedesktop.dbus.test.helper.interfaces.Binding;
import org.freedesktop.dbus.test.helper.interfaces.SamplesInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

public class TestCross extends AbstractDBusBaseTest {

    private ServerThread serverThread;
    private CrossTestServer cts;

    private volatile boolean serverReady = false;

    @BeforeEach
    public void before() {
        serverThread = new ServerThread();
        serverThread.setName("Server Thread");
        serverThread.start();
    }

    @AfterEach
    public void after() {
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Test
    public void testCross() throws InterruptedException {
        while (!serverReady) {
            Thread.sleep(500L);
        }

        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            /* init */
            CrossTestClient client = new CrossTestClient(conn);
            conn.exportObject("/TestClient", client);
            conn.addSigHandler(Binding.SampleSignals.Triggered.class, client);
            SamplesInterface tests = conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/Test", SamplesInterface.class);
            Binding.SingleSample singletests = conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/Test", Binding.SingleSample.class);
            Peer peer = conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/Test", Peer.class);
            Introspectable intro = conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/Test", Introspectable.class);

            Introspectable rootintro = conn.getRemoteObject("org.freedesktop.DBus.Binding.TestServer", "/", Introspectable.class);

            client.doTests(peer, intro, rootintro, tests, singletests);

            /* report results */
            for (String s : client.getPassed()) {
                logger.info(s + " pass");
            }

            for (Entry<String, List<String>> s : client.getFailed().entrySet()) {
                for (String msg : s.getValue()) {
                    logger.error(s.getKey() + " failed: " + msg);
                }
            }

        } catch (DBusException | IOException _exDbe) {
            _exDbe.printStackTrace();
            fail("Exception while processing DBus");
        }

        while (serverThread.isAlive()) {
            Thread.sleep(300L);
        }

        assertTrue(cts.getNotdone().isEmpty(), "All tests should have been run, following failed: " + String.join(", ", cts.getNotdone()));

    }

    private class ServerThread extends Thread {
        @Override
        public void run() {
            try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {

                conn.requestBusName("org.freedesktop.DBus.Binding.TestServer");
                cts = new CrossTestServer(conn);
                conn.addSigHandler(Binding.SampleClient.Trigger.class, cts);
                conn.exportObject("/Test", cts);
                serverReady = true;
                while (cts.isRun()) {
                    try {
                        //cts.wait();
                        Thread.sleep(500L);
                    } catch (InterruptedException _exIe) {
                    }
                }
                for (String s : cts.getDone()) {
                    logger.info(s + " ok");
                }
                for (String s : cts.getNotdone()) {
                    logger.warn("---> " + s + " untested");
                }
                conn.disconnect();
            } catch (DBusException | IOException _exDe) {
                _exDe.printStackTrace();
                fail("Exception while server running");
            }
        }
    }
}
