package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ObjectManagerTest extends AbstractDBusBaseTest {

    @Test
    void testAutoGetManagedObjects() throws Exception {
        serverconn.exportObjectManager("/com/acme");
        serverconn.exportObject(new DeviceImpl("/com/acme/dev1", "Device One"));

        ObjectManager mgr = clientconn.getRemoteObject(getTestBusName(), "/com/acme", ObjectManager.class);
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> managed = mgr.GetManagedObjects();

        DBusPath devPath = new DBusPath("/com/acme/dev1");
        assertTrue(managed.containsKey(devPath), "managed objects must contain the child, got: " + managed.keySet());
        Map<String, Map<String, Variant<?>>> ifaces = managed.get(devPath);
        assertTrue(ifaces.containsKey("com.acme.Device1"), "interface missing, got: " + ifaces.keySet());
        assertEquals("Device One", ifaces.get("com.acme.Device1").get("Name").getValue());
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testAutoInterfacesAddedAndRemoved() throws Exception {
        serverconn.exportObjectManager("/com/acme2");

        CountDownLatch addedLatch = new CountDownLatch(1);
        AtomicReference<ObjectManager.InterfacesAdded> addedRef = new AtomicReference<>();
        clientconn.addSigHandler(ObjectManager.InterfacesAdded.class, s -> {
            if ("/com/acme2/dev1".equals(s.getSignalSource().getPath())) {
                addedRef.set(s);
                addedLatch.countDown();
            }
        });

        serverconn.exportObject(new DeviceImpl("/com/acme2/dev1", "Dev"));

        assertTrue(addedLatch.await(15, TimeUnit.SECONDS), "InterfacesAdded not received");
        assertTrue(addedRef.get().getInterfaces().containsKey("com.acme.Device1"),
            "added signal must list the interface, got: " + addedRef.get().getInterfaces().keySet());

        CountDownLatch removedLatch = new CountDownLatch(1);
        AtomicReference<ObjectManager.InterfacesRemoved> removedRef = new AtomicReference<>();
        clientconn.addSigHandler(ObjectManager.InterfacesRemoved.class, s -> {
            if ("/com/acme2/dev1".equals(s.getSignalSource().getPath())) {
                removedRef.set(s);
                removedLatch.countDown();
            }
        });

        serverconn.unExportObject("/com/acme2/dev1");

        assertTrue(removedLatch.await(15, TimeUnit.SECONDS), "InterfacesRemoved not received");
        assertTrue(removedRef.get().getInterfaces().contains("com.acme.Device1"),
            "removed signal must list the interface, got: " + removedRef.get().getInterfaces());
    }

    @Test
    void testManualObjectManagerUsesOwnImplementation() throws Exception {
        try (DBusConnection manualConn = DBusConnectionBuilder.forSessionBus().withShared(false)
            .withManualObjectManager(true).build()) {
            manualConn.requestBusName("com.acme.manual");
            manualConn.exportObject(new CustomObjectManager("/mgr"));

            try (DBusConnection reader = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
                ObjectManager mgr = reader.getRemoteObject("com.acme.manual", "/mgr", ObjectManager.class);
                Map<DBusPath, Map<String, Map<String, Variant<?>>>> managed = mgr.GetManagedObjects();

                // the custom implementation returns a sentinel entry; the library must NOT intercept it
                assertTrue(managed.containsKey(new DBusPath("/custom/sentinel")),
                    "manual ObjectManager implementation must be used, got: " + managed.keySet());
            }
        }
    }

    @DBusInterfaceName("com.acme.Device1")
    public interface Device extends DBusInterface {
        @DBusBoundProperty(access = Access.READ, name = "Name")
        String getName();
    }

    public static class DeviceImpl implements Device {
        private final String path;
        private final String name;

        public DeviceImpl(String _path, String _name) {
            path = _path;
            name = _name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getObjectPath() {
            return path;
        }
    }

    public static class CustomObjectManager implements ObjectManager {
        private final String path;

        public CustomObjectManager(String _path) {
            path = _path;
        }

        @Override
        public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects() {
            return Map.of(new DBusPath("/custom/sentinel"), Map.of("com.acme.Sentinel", Map.of()));
        }

        @Override
        public String getObjectPath() {
            return path;
        }
    }
}
