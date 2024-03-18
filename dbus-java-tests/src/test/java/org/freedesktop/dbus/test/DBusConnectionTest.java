package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidInterfaceSignature;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DBusConnectionTest extends AbstractDBusDaemonBaseTest {

    @Test
    public void testBusnamesShouldBeAutoReleasedOnCloseOfNonSharedConnection() throws Exception {

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

    /**
     * Test verifies that interfaces in object hierarchy which are not public will throw an exception.
     */
    @Test
    public void testExportOnlyAllPublic() throws Exception {
        try (var conn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
            ExportedObj exportedObj = new ExportedObj();
            conn.requestBusName(getClass().getName());
            assertThrows(InvalidInterfaceSignature.class, () -> conn.exportObject(exportedObj));
        }
    }

    interface NonPublicInterface extends DBusInterface {

    }

    public static class ExportedObj implements NonPublicInterface {

        @Override
        public String getObjectPath() {
            return "/" + getClass().getSimpleName().toLowerCase();
        }

    }

}
