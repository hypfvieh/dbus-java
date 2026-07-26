package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.errors.PropertyReadOnly;
import org.freedesktop.dbus.errors.UnknownProperty;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Verifies the additional standard {@code org.freedesktop.DBus} bus features:
 * <ul>
 *   <li>D3 - the {@code ActivatableServicesChanged} signal is declared/introspected</li>
 *   <li>D4 - the {@code ReloadConfig} method exists (no-op on the embedded daemon)</li>
 *   <li>D5 - the bus properties {@code Features} and {@code Interfaces}</li>
 * </ul>
 */
class BusInterfaceExtrasTest extends AbstractBaseTest {

    private static final String DBUS_BUSNAME = "org.freedesktop.DBus";
    private static final String DBUS_BUSPATH = "/org/freedesktop/DBus";

    @Test
    void testBusInterfaceExtrasOnDefaultDaemon() throws Exception {
        withDaemon(false, conn -> {
            // D3 + D5: the introspection data declares the new signal, the Properties interface and the properties
            Introspectable intro = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Introspectable.class);
            String xml = intro.Introspect();
            assertTrue(xml.contains("ActivatableServicesChanged"), "ActivatableServicesChanged signal missing");
            assertTrue(xml.contains("org.freedesktop.DBus.Properties"), "Properties interface missing");
            assertTrue(xml.contains("name=\"Features\""), "Features property missing");
            assertTrue(xml.contains("name=\"Interfaces\""), "Interfaces property missing");

            // D4: ReloadConfig is a no-op and must not fail
            DBus dbus = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, DBus.class);
            assertDoesNotThrow(dbus::ReloadConfig);

            // D5: Features is empty, Interfaces contains Monitoring but not Debug.Stats (debug disabled)
            Properties props = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Properties.class);
            assertEquals(List.of(), asStringList(props.Get(DBUS_BUSNAME, "Features")));

            List<String> interfaces = asStringList(props.Get(DBUS_BUSNAME, "Interfaces"));
            assertTrue(interfaces.contains("org.freedesktop.DBus.Monitoring"), "Monitoring should be listed");
            assertFalse(interfaces.contains("org.freedesktop.DBus.Debug.Stats"), "Debug.Stats must not be listed on default daemon");

            // GetAll returns both properties
            Map<String, Variant<?>> all = props.GetAll(DBUS_BUSNAME);
            assertTrue(all.containsKey("Features"), "GetAll missing Features");
            assertTrue(all.containsKey("Interfaces"), "GetAll missing Interfaces");

            // unknown property -> error, properties are read only
            assertThrows(UnknownProperty.class, () -> props.Get(DBUS_BUSNAME, "DoesNotExist"));
            assertThrows(PropertyReadOnly.class, () -> props.Set(DBUS_BUSNAME, "Features", new String[0]));
        });
    }

    @Test
    void testInterfacesPropertyListsDebugStatsOnDebuggableDaemon() throws Exception {
        withDaemon(true, conn -> {
            Properties props = conn.getRemoteObject(DBUS_BUSNAME, DBUS_BUSPATH, Properties.class);
            List<String> interfaces = asStringList(props.Get(DBUS_BUSNAME, "Interfaces"));
            assertTrue(interfaces.contains("org.freedesktop.DBus.Debug.Stats"),
                "Debug.Stats should be listed on a debuggable daemon, got: " + interfaces);
        });
    }

    /**
     * Normalizes a value that may be a {@link Variant} wrapping an array/list of strings (or the array/list directly)
     * into a {@link List} of strings.
     */
    private static List<String> asStringList(Object _value) {
        Object val = _value instanceof Variant<?> v ? v.getValue() : _value;
        List<String> result = new ArrayList<>();
        if (val instanceof Object[] arr) {
            for (Object o : arr) {
                result.add(String.valueOf(o));
            }
        } else if (val instanceof Collection<?> col) {
            for (Object o : col) {
                result.add(String.valueOf(o));
            }
        }
        return result;
    }

    private void withDaemon(boolean _debug, ConnectionConsumer _consumer) throws IOException {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);
        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        try (EmbeddedDBusDaemon daemon = _debug
                ? new DebuggableEmbeddedDBusDaemon(listenBusAddress)
                : new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress).withShared(false).build()) {
                _consumer.accept(conn);
            } catch (Exception _ex) {
                fail(_ex);
            }
        }
    }

    @FunctionalInterface
    private interface ConnectionConsumer {
        void accept(DBusConnection _conn) throws Exception;
    }
}
