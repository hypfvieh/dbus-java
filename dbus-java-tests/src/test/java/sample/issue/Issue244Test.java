package sample.issue;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

/**
 * Test case to verify that shared connections do not block reconnect.
 * When a client uses shared connections and gets disconnected because
 * the daemon disappears, the shared connection should no longer be used.
 * Instead this broken connection should be cleaned from the internal connection map.
 */
public class Issue244Test extends AbstractBaseTest {

    @Test
    public void testSharedConnection() {
        String busType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String addr = TransportBuilder.createDynamicSession(busType, false);
        BusAddress clientAddress = BusAddress.of(addr);
        BusAddress serverAddress = BusAddress.of(addr).getListenerAddress();

        logger.info("Creating {} based DBus daemon on address {}", busType, serverAddress);

        try (EmbeddedDBusDaemon edbus = new EmbeddedDBusDaemon(serverAddress)) {
            edbus.startInBackgroundAndWait(MAX_WAIT);
            try (DBusConnection con = DBusConnectionBuilder.forAddress(clientAddress).build()) {
                assertTrue(con.isConnected(), "First connection attempt must work");
                edbus.close(); // terminate daemon to enforce disconnect
                Thread.sleep(1000L);
                assertFalse(con.isConnected(), "No connection expected after first disconnect"); // no connection without daeon
            }

            // restart daemon and retry
            edbus.startInBackgroundAndWait(MAX_WAIT);
            try (DBusConnection con = DBusConnectionBuilder.forAddress(clientAddress).build()) {
                assertTrue(con.isConnected(), "Second connection attempt must work");
                edbus.close(); // terminate daemon to enforce disconnect
                Thread.sleep(1000L);
                assertFalse(con.isConnected(), "No connection expected after second disconnect"); // no connection without daeon
            }

        } catch (Exception _ex) {
            fail("No exception expected", _ex);
        }

    }
}
