package org.freedesktop.dbus.bin;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.utils.AddressBuilder;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class EmbeddedDBusDaemonTest extends AbstractBaseTest {

    @Test
    public void testStartAndConnectEmbeddedDBusDaemon() throws DBusException {
        String protocolType = TransportBuilder.getRegisteredBusTypes().get(0);
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);

        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        logger.debug("Starting embedded bus on address {})", listenBusAddress);
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackground();
            logger.debug("Started embedded bus on address {}", listenBusAddress);

            waitForDaemon(daemon);

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
    public void test_start_stop() throws Exception {

        for (int i = 0; i < 2; i++) {
            String address = TransportBuilder.createDynamicSession(TransportBuilder.getRegisteredBusTypes().get(0), true);
            BusAddress busAddress = BusAddress.of(address);

            // initialize
            EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(busAddress);

            if (busAddress.isBusType("TCP")) {
                String addrStr  = busAddress.removeParameter("listen").toString();
                System.setProperty(AddressBuilder.DBUS_SESSION_BUS_ADDRESS, addrStr);
            }

            // start the daemon in background to not block the test
            AtomicReference<Exception> exception = new AtomicReference<>();
            Thread daemonThread = new Thread(() -> {
                try {
                    daemon.startInForeground();
                } catch (Exception ex) {
                    exception.set(ex);
                    ex.printStackTrace();
                }
            });
            daemonThread.start();

            // give the daemon time to start
            Thread.sleep(1000);
            daemon.close();
            assertEquals(null, exception.get()); // assertEquals() gives a better error message
        }
    }
}
