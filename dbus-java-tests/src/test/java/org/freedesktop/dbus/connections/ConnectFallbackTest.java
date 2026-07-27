package org.freedesktop.dbus.connections;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the connect-fallback over multiple bus addresses (A1b): the addresses are tried in order until one
 * connects; if none connects, the build fails.
 */
class ConnectFallbackTest extends AbstractBaseTest {

    /** A fresh, unused endpoint of the active transport (nothing is listening there). */
    private static String deadAddress() {
        return TransportBuilder.createDynamicSession(TransportBuilder.getRegisteredBusTypes().getFirst(), false);
    }

    @Test
    void testFallbackToSecondAddress() throws Exception {
        String type = TransportBuilder.getRegisteredBusTypes().getFirst();
        String liveBase = TransportBuilder.createDynamicSession(type, false);
        BusAddress dead = BusAddress.of(deadAddress());
        BusAddress live = BusAddress.of(liveBase);

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(BusAddress.of(liveBase + ",listen=true"))) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddresses(dead, live)
                    .transportConfig().withTimeout(1000).back()
                    .withShared(false).build()) {
                assertNotNull(conn.getUniqueName(), "should have connected via the second (live) address");
            }
        }
    }

    @Test
    void testFallbackViaSemicolonSeparatedString() throws Exception {
        String type = TransportBuilder.getRegisteredBusTypes().getFirst();
        String liveBase = TransportBuilder.createDynamicSession(type, false);
        String combined = deadAddress() + ";" + liveBase;

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(BusAddress.of(liveBase + ",listen=true"))) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(combined)
                    .transportConfig().withTimeout(1000).back()
                    .withShared(false).build()) {
                assertNotNull(conn.getUniqueName(), "should have connected via the live address of the list");
            }
        }
    }

    @Test
    void testAllAddressesFail() {
        BusAddress dead1 = BusAddress.of(deadAddress());
        BusAddress dead2 = BusAddress.of(deadAddress());

        assertThrows(DBusException.class, () -> {
            try (DBusConnection conn = DBusConnectionBuilder.forAddresses(dead1, dead2)
                    .transportConfig().withTimeout(1000).back()
                    .withShared(false).build()) {
                fail("connection must fail when no address is reachable");
            }
        });
    }

    @Test
    void testSingleAddressStillWorks() throws Exception {
        String type = TransportBuilder.getRegisteredBusTypes().getFirst();
        String liveBase = TransportBuilder.createDynamicSession(type, false);

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(BusAddress.of(liveBase + ",listen=true"))) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddresses(BusAddress.of(liveBase))
                    .withShared(false).build()) {
                assertNotNull(conn.getUniqueName(), "single-address connect should still work");
            }
        }
    }
}
