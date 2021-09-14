package org.freedesktop.dbus.test;

import java.io.IOException;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base test which will start a embedded DBus daemon if no UNIX transport is found.
 * 
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-14
 */
public class AbstractDbusBaseTest extends AbstractBaseTest {

    protected static EmbeddedDBusDaemon edbus;

    @BeforeAll
    public static void beforeAll() throws DBusException {
        if (!TransportFactory.getRegisteredBusTypes().contains("UNIX")) {
            edbus = new EmbeddedDBusDaemon();
            String addr = TransportFactory.createDynamicSession(TransportFactory.getRegisteredBusTypes().get(0), true);
            edbus.setAddress(addr);
            edbus.startInBackground();
        }
    }

    @AfterAll
    public static void afterAll() throws IOException {
        if (edbus != null) {
            edbus.close();
        }
    }
}
