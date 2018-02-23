package org.freedesktop.dbus.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.DBus.ObjectManager")
public interface ObjectManager extends DBusInterface {
    /**
     * Get a sub-tree of objects. The root of the sub-tree is this object.
     * @return A Map from object path (DBusInterface) to a Map from interface name to a properties Map (as returned by Properties.GetAll())
     */
    Map<String, Map<String, Map<String, Variant<?>>>> GetManagedObjects();

    /**
     * Signal generated when a new interface is added
     */
    class InterfacesAdded extends DBusSignal {
        public final String object;
        public final Map<String, Map<String, Variant<?>>> interfaces;

        public InterfacesAdded(String path, String object, Map<String, Map<String, Variant<?>>> interfaces) throws DBusException {
            super(path, object, interfaces);
            this.object = object;
            this.interfaces = interfaces;
        }

        public String getObject() {
            return object;
        }

        public Map<String, Map<String, Variant<?>>> getInterfaces() {
            return interfaces;
        }
        
        
    }

    /**
     * Signal generated when an interface is removed
     */
    class InterfacesRemoved extends DBusSignal {
        public final String object;
        public final List<String> interfaces;

        public InterfacesRemoved(String path, String object, List<String> interfaces) throws DBusException {
            super(path, object, interfaces);
            this.object = object;
            this.interfaces = interfaces;
        }

        public String getObject() {
            return object;
        }

        public List<String> getInterfaces() {
            return interfaces;
        }
        
        
    }

}