package org.freedesktop.dbus.bin;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class EmbeddedDBusDaemonTest extends AbstractBaseTest {
    /** Max wait time to wait for daemon to start. */
    private static final long MAX_WAIT = Duration.ofSeconds(30).toMillis();

    @Test
    public void testStartAndConnectEmbeddedDBusDaemon() throws DBusException {
        String protocolType = TransportBuilder.getRegisteredBusTypes().get(0);
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);

        BusAddress busAddress = new BusAddress(newAddress);
        BusAddress listenBusAddress = new BusAddress(newAddress + ",listen=true");

        logger.debug("Starting embedded bus on address {})", listenBusAddress.getRawAddress());
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackground();
            logger.debug("Started embedded bus on address {}", listenBusAddress.getRawAddress());

            long sleepMs = 200;
            long waited = 0;

            while (!daemon.isRunning()) {
                if (waited >= MAX_WAIT) {
                    throw new RuntimeException("EmbeddedDbusDaemon not started in the specified time of " + MAX_WAIT + " ms");
                }

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException _ex) {
                    break;
                }

                waited += sleepMs;
                logger.debug("Waiting for embedded daemon to start: {} of {} ms waited", waited, MAX_WAIT);
            }

            // connect to started daemon process
            logger.info("Connecting to embedded DBus {}", busAddress.getRawAddress());

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress.getRawAddress()).build()) {
                logger.debug("Connected to embedded DBus {}", busAddress.getRawAddress());
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

            // initialize
            EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(address);

            // start the daemon in background to not block the test
            AtomicReference<Exception> exception = new AtomicReference<>();
            Thread daemonThread = new Thread(() -> {
                try {
                    daemon.startInForeground();
                } catch (Exception ex) {
                    exception.set(ex);
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
