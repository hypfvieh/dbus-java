package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartInterface;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartObject;
import org.freedesktop.dbus.test.helper.twopart.TwoPartTestClient.TwoPartTestObject;
import org.freedesktop.dbus.test.helper.twopart.TwoPartTestServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestTwoPart extends AbstractDBusDaemonBaseTest {

    private volatile boolean serverReady = false;

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void testTwoPart() throws InterruptedException {
        TwoPartServer twoPartServer = new TwoPartServer();
        twoPartServer.start();

        while (!serverReady) {
            Thread.sleep(1500L);
        }

        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {

            logger.debug("get remote");
            TwoPartInterface remote = conn.getRemoteObject("org.freedesktop.dbus.test.two_part_server", "/", TwoPartInterface.class);

            assertNotNull(remote);

            logger.debug("get object");
            TwoPartObject o = remote.getNew();
            assertNotNull(o);

            logger.debug("get name");
            assertEquals("give name", o.getName());

            TwoPartTestObject tpto = new TwoPartTestObject();
            conn.exportObject("/TestObject", tpto);

            TwoPartInterface.TwoPartSignal message = new TwoPartInterface.TwoPartSignal("/FromObject", tpto);
            long signalSerial = message.getSerial();
            conn.sendMessage(message);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException _ex) {
            }

            // when a signal is received, a new signal object is created
            // this will take the next available global serial even this message
            // is never transmitted on the bus.
            // Therefore it is expected that the serial stored before is one step lower than the
            // serial received in the handler
            signalSerial++;
            assertEquals(signalSerial, twoPartServer.receivedSignalSerial, "Expected signal serial to be the same");
        } catch (DBusException | IOException _ex) {
            fail("Exception in client", _ex);
        }
    }

    private class TwoPartServer extends Thread {

        private long receivedSignalSerial;

        TwoPartServer() {
            super("TwoPartServerThread");
            setDaemon(true);
        }

        @SuppressWarnings("PMD.EmptyCatchBlock")
        @Override
        public void run() {
            try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {

                conn.requestBusName("org.freedesktop.dbus.test.two_part_server");
                TwoPartTestServer server = new TwoPartTestServer(conn);
                conn.exportObject("/", server);
                conn.addSigHandler(TwoPartInterface.TwoPartSignal.class, server);
                serverReady = true;
                do {
                    try {
                        sleep(200L);
                    } catch (InterruptedException _ex) {
                    }
                } while (server.getSignalSerial() == 0);

                // the serial number of the signal we received
                // the signal was created before and should have the same serial
                receivedSignalSerial = server.getSignalSerial();
            } catch (DBusException | IOException _ex) {
                logger.error("Exception while running TwoPartServer", _ex);
                throw new RuntimeException("Exception in server");
            }

        }

    }
}
