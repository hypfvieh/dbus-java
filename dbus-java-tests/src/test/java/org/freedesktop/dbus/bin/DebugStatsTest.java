package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.errors.ServiceUnknown;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Debug;
import org.freedesktop.dbus.interfaces.Verbose;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * Verifies the {@code org.freedesktop.DBus.Debug.Stats} interface which is only offered by the
 * {@link DebuggableEmbeddedDBusDaemon}, not by the default {@link EmbeddedDBusDaemon}.
 */
class DebugStatsTest extends AbstractBaseTest {

    private static final String DBUS_BUSNAME = "org.freedesktop.DBus";
    private static final String DBUS_BUSPATH = "/org/freedesktop/DBus";

    @Test
    void testDebugStatsAvailableOnDebuggableDaemon() throws Exception {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);
        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        try (DebuggableEmbeddedDBusDaemon daemon = new DebuggableEmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress).withShared(false).build()) {
                Debug.Stats stats = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Debug.Stats.class);

                // global statistics contain the documented keys
                Map<String, Variant<?>> global = stats.GetStats();
                assertTrue(global.containsKey("ActiveConnections"), "ActiveConnections missing");
                assertTrue(global.containsKey("BusNames"), "BusNames missing");
                assertTrue(global.containsKey("MatchRules"), "MatchRules missing");
                assertInstanceOf(UInt32.class, global.get("ActiveConnections").getValue());

                // add a match rule and ensure it is reported by GetAllMatchRules
                DBus dbus = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, DBus.class);
                dbus.AddMatch("type='signal',interface='com.example.Foo'");

                Map<String, String[]> allRules = stats.GetAllMatchRules();
                boolean found = allRules.values().stream().flatMap(Arrays::stream)
                    .anyMatch(r -> r.contains("com.example.Foo"));
                assertTrue(found, "added match rule should be listed by GetAllMatchRules");

                // per-connection statistics for our own unique name
                Map<String, Variant<?>> connStats = stats.GetConnectionStats(conn.getUniqueName());
                assertEquals(conn.getUniqueName(), connStats.get("UniqueName").getValue());

                // unknown connection name -> error
                assertThrows(ServiceUnknown.class, () -> stats.GetConnectionStats("com.does.not.Exist"));

                // org.freedesktop.DBus.Verbose is available too when debug features are enabled
                Verbose verbose = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Verbose.class);
                assertDoesNotThrow(verbose::EnableVerbose);
                assertDoesNotThrow(verbose::DisableVerbose);
            }
        }
    }

    @Test
    void testDebugStatsUnavailableOnDefaultDaemon() throws Exception {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);
        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress).withShared(false).build()) {
                Debug.Stats stats = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Debug.Stats.class);
                // a default daemon behaves like a production reference daemon: the interfaces do not exist
                assertThrows(UnknownMethod.class, stats::GetStats);

                Verbose verbose = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Verbose.class);
                assertThrows(UnknownMethod.class, verbose::EnableVerbose);
            }
        }
    }
}
