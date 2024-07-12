package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.config.DBusSysProps;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
class EmbeddedDBusDaemonTest extends AbstractBaseTest {

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testStartAndConnectEmbeddedDBusDaemon() throws DBusException {
        String protocolType = TransportBuilder.getRegisteredBusTypes().get(0);
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);

        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        logger.debug("Starting embedded bus on address {})", listenBusAddress);
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            logger.debug("Started embedded bus on address {}", listenBusAddress);
            daemon.startInBackgroundAndWait(MAX_WAIT);

            // connect to started daemon process
            logger.info("Connecting to embedded DBus {}", busAddress);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress).build()) {
                logger.debug("Connected to embedded DBus {}", busAddress);
            } catch (Exception _ex) {
                fail("Connection to EmbeddedDbusDaemon failed", _ex);
                logger.error("Error connecting to EmbeddedDbusDaemon", _ex);
            }
        } catch (IOException _ex1) {
            fail("Failed to start EmbeddedDbusDaemon", _ex1);
            logger.error("Error starting EmbeddedDbusDaemon", _ex1);
        }
    }

    @Test
    void testStartStop() throws Exception {

        for (int i = 0; i < 2; i++) {
            String address = TransportBuilder.createDynamicSession(TransportBuilder.getRegisteredBusTypes().get(0), true);
            BusAddress busAddress = BusAddress.of(address);

            // initialize
            EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(busAddress);

            if (busAddress.isBusType("TCP")) {
                String addrStr  = busAddress.removeParameter("listen").toString();
                System.setProperty(DBusSysProps.DBUS_SESSION_BUS_ADDRESS, addrStr);
            }

            // start the daemon in background to not block the test
            AtomicReference<Exception> exception = new AtomicReference<>();
            Thread daemonThread = new Thread(() -> {
                try {
                    daemon.startInForeground();
                } catch (Exception _ex) {
                    exception.set(_ex);
                    _ex.printStackTrace();
                }
            });
            daemonThread.start();

            // give the daemon time to start
            Thread.sleep(1000);
            daemon.close();
            assertEquals(null, exception.get()); // assertEquals() gives a better error message
        }

        Entry<Thread, StackTraceElement[]> elems = null;
        for (Entry<Thread, StackTraceElement[]> stacks : Thread.getAllStackTraces().entrySet()) {
            if (stacks.getKey().getName().startsWith(DBusDaemon.class.getSimpleName())) {
                elems = stacks;
                break;
            }
        }

        if (elems != null) {
            System.out.println("Found possibly running instances: " + elems.getKey().getName());
            for (StackTraceElement st : elems.getValue()) {
                System.out.println("\t" + st.toString());
            }
            fail("All dbus daemon threads should have been terminated");
        }
    }
}
