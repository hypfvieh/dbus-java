package org.freedesktop.dbus.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.DBus.ObjectManager")
public interface ObjectManager extends DBusInterface {
    /**
     * Get a sub-tree of objects. The root of the sub-tree is this object.
     * 
     * @return A Map from object path (DBusInterface) to a Map from interface name to a properties Map (as returned by
     *         Properties.GetAll())
     */
    Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects();

    /**
     * Signal generated when a new interface is added
     */
    public static class InterfacesAdded extends DBusSignal {
        public final DBusPath                             dbusInterface;
        public final String                               objectPath;

        public final Map<String, Map<String, Variant<?>>> interfaces;

        public InterfacesAdded(String _objectPath, DBusPath _interface, Map<String, Map<String, Variant<?>>> interfaces)
                throws DBusException {
            super(_objectPath, _interface, interfaces);
            this.objectPath = _objectPath;
            this.dbusInterface = _interface;
            this.interfaces = interfaces;
        }

        public DBusPath getDbusInterface() {
            return dbusInterface;

        }

        public String getObjectPath() {
            return objectPath;
        }

        public Map<String, Map<String, Variant<?>>> getInterfaces() {
            return interfaces;
        }

    }

    /**
     * Signal generated when an interface is removed
     */
    public static class InterfacesRemoved extends DBusSignal {
        public final DBusPath     dbusInterface;
        public final String       objectPath;

        public final List<String> interfaces;

        public InterfacesRemoved(String _objectPath, DBusPath _interface, List<String> interfaces)
                throws DBusException {
            super(_objectPath, _interface, interfaces);
            this.objectPath = _objectPath;
            this.dbusInterface = _interface;
            this.interfaces = interfaces;
        }

        public DBusPath getDbusInterface() {
            return dbusInterface;
        }

        public String getObjectPath() {
            return objectPath;
        }

        public List<String> getInterfaces() {
            return interfaces;
        }

    }

}
