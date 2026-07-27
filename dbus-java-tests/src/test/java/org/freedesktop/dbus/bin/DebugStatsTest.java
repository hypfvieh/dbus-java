package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.errors.ServiceUnknown;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Debug;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Verbose;
import org.freedesktop.dbus.test.AbstractEmbeddedDaemonTest;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * Verifies the {@code org.freedesktop.DBus.Debug.Stats} interface which is only offered by the
 * {@link DebuggableEmbeddedDBusDaemon}, not by the default {@link EmbeddedDBusDaemon}.
 */
class DebugStatsTest extends AbstractEmbeddedDaemonTest {

    private static final String DBUS_BUSNAME = "org.freedesktop.DBus";
    private static final String DBUS_BUSPATH = "/org/freedesktop/DBus";

    @Test
    void testDebugStatsAvailableOnDebuggableDaemon() throws Exception {
        withEmbeddedConnection(DebuggableEmbeddedDBusDaemon::new, conn -> {
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

            // org.freedesktop.DBus.Verbose is available too when debug features are enabled ...
            Verbose verbose = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Verbose.class);
            assertDoesNotThrow(verbose::EnableVerbose);
            assertDoesNotThrow(verbose::DisableVerbose);

            // ... and is advertised via introspection
            Introspectable intro = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Introspectable.class);
            assertTrue(intro.Introspect().contains("org.freedesktop.DBus.Verbose"),
                "Verbose interface should be introspected on a debuggable daemon");
        });
    }

    @Test
    void testDebugStatsUnavailableOnDefaultDaemon() throws Exception {
        withEmbeddedConnection(conn -> {
            Debug.Stats stats = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Debug.Stats.class);
            // a default daemon behaves like a production reference daemon: the interfaces do not exist
            assertThrows(UnknownMethod.class, stats::GetStats);

            Verbose verbose = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Verbose.class);
            assertThrows(UnknownMethod.class, verbose::EnableVerbose);

            // the default daemon must not advertise the Verbose interface via introspection either
            Introspectable intro = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Introspectable.class);
            assertFalse(intro.Introspect().contains("org.freedesktop.DBus.Verbose"),
                "Verbose interface must not be introspected on a default daemon");
        });
    }
}
