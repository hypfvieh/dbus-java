package org.freedesktop.dbus.test;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MapOfVariantTest extends AbstractDBusBaseTest {

    @Test
    public void testMapOfVariant() throws IOException, DBusException {
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            MyVariantObject myVariantObject = new MyVariantObject();
            conn.requestBusName("org.freedesktop.dbus.test");
            conn.exportObject(myVariantObject);

            MapVariant remoteObject = conn.getRemoteObject("org.freedesktop.dbus.test", "/org/freedesktop/dbus/test/MyVariantObject", MapVariant.class);

            // check content of the map retrieved on the Bus
            Map<String, Variant<?>> mapOfVariant = remoteObject.getMapOfVariant();
            assertTrue(mapOfVariant.containsKey("SomeString"));
            assertTrue(mapOfVariant.containsKey("SomeNumber"));
            assertEquals("Something", mapOfVariant.get("SomeString").getValue());
            assertEquals(1, mapOfVariant.get("SomeNumber").getValue());

            // overwrite the exported map
            Map<String, Variant<?>> newMap = new HashMap<>();
            newMap.put("overwrittenString", new Variant<>("other value"));
            newMap.put("overwrittenNumber", new Variant<>(1000));
            remoteObject.setMapOfVariant(newMap);

            // check content again
            mapOfVariant = remoteObject.getMapOfVariant();

            assertTrue(mapOfVariant.containsKey("overwrittenString"));
            assertTrue(mapOfVariant.containsKey("overwrittenNumber"));
            assertEquals("other value", mapOfVariant.get("overwrittenString").getValue());
            assertEquals(1000, mapOfVariant.get("overwrittenNumber").getValue());
        }

    }

    @DBusInterfaceName("org.freedesktop.dbus.test.MapVariant")
    public interface MapVariant extends DBusInterface {

        Map<String, Variant<?>> getMapOfVariant();

        void setMapOfVariant(Map<String, Variant<?>> _map);
    }

    public static class MyVariantObject implements MapVariant {

        private Map<String, Variant<?>> variantMap = new HashMap<>();

        public MyVariantObject() {
            variantMap.put("SomeString", new Variant<>("Something"));
            variantMap.put("SomeNumber", new Variant<>(1));
        }

        @Override
        public String getObjectPath() {
            return "/org/freedesktop/dbus/test/MyVariantObject";
        }

        @Override
        public Map<String, Variant<?>> getMapOfVariant() {
            return variantMap;
        }

        @Override
        public void setMapOfVariant(Map<String, Variant<?>> _map) {
            variantMap = _map;
        }

    }
}
