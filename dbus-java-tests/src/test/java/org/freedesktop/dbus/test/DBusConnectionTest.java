package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DBusConnectionTest extends AbstractDBusBaseTest {

    @Test
    public void test_busnames_should_be_auto_released_on_close_of_non_shared_connection() throws Exception {

        // prepare 2 independent connection
        String busName = "org.freedesktop.dbus.test.TestBus";
        
        DBusConnection connection1 = DBusConnectionBuilder.forSessionBus().withShared(false).build();
        DBusConnection connection2 = DBusConnectionBuilder.forSessionBus().withShared(false).build();
        assertNotEquals(connection1.getUniqueName(), connection2.getUniqueName());

        // only one connection can have the bus
        connection1.requestBusName(busName);
        assertThrows(DBusException.class, () -> connection2.requestBusName(busName));

        // after a close of conn1 the bus name should be available again
        connection1.close();
        connection2.requestBusName(busName);
        connection2.close();
    }
}
