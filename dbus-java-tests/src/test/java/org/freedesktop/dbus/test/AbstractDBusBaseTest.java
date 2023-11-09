package org.freedesktop.dbus.test;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.config.DBusSysProps;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Base test which will start a embedded DBus daemon if no UNIX transport is found.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-14
 */
public abstract class AbstractDBusBaseTest extends AbstractBaseTest {

    //CHECKSTYLE:OFF
    protected static EmbeddedDBusDaemon edbus;
    //CHECKSTYLE:ON

    /**
     * Wait 500 ms if the current test uses TCP transport.
     *
     * @throws InterruptedException on interruption
     */
    protected static void waitIfTcp() throws InterruptedException {
        if (!TransportBuilder.getRegisteredBusTypes().contains("UNIX")) {
            Thread.sleep(500L);
        }
    }

    /**
     * Start an embedded Dbus daemon (in background) if the test uses TCP transport.
     *
     * @throws DBusException if start of daemon failed
     * @throws InterruptedException on interruption
     */
    @BeforeAll
    public static void beforeAll() throws DBusException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(AbstractDBusBaseTest.class);
        if (!TransportBuilder.getRegisteredBusTypes().contains("UNIX")) {
            String busType = TransportBuilder.getRegisteredBusTypes().get(0);
            String addr = TransportBuilder.createDynamicSession(busType, true);
            BusAddress address = BusAddress.of(addr);

            logger.info("Creating {} based DBus daemon on address {}", busType, addr);
            edbus = new EmbeddedDBusDaemon(addr);
            edbus.startInBackgroundAndWait(MAX_WAIT);

            if (address.isBusType("TCP")) {
                String addrStr  = address.removeParameter("listen").toString();
                System.setProperty(DBusSysProps.DBUS_SESSION_BUS_ADDRESS, addrStr);
            }

        }
    }

    /**
     * Shutdown embedded Dbus daemon after test (if any).
     *
     * @throws IOException shutdown failed
     */
    @AfterAll
    public static void afterAll() throws IOException {
        if (edbus != null) {
            edbus.close();
        }
    }
}
