package org.freedesktop.dbus.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * Extended DBus-Properties object which supports the PropertiesChanged signal of DBus.
 *
 * @author hypfvieh
 *
 */
@DBusInterfaceName("org.freedesktop.DBus.Properties")
public interface SignalAwareProperties extends Properties {

    public static class PropertiesChanged extends DBusSignal {
        private final Map<String, Variant<?>> propertiesChanged;
        private final List<String>         propertiesRemoved;
        
        private final String interfaceName;
        

        public PropertiesChanged(String _path, String _interfaceName, Map<String, Variant<?>> _propertiesChanged, List<String> _propertiesRemoved) throws DBusException {
            super(_path, _interfaceName, _propertiesChanged, _propertiesRemoved);

            this.propertiesChanged = _propertiesChanged;
            this.propertiesRemoved = _propertiesRemoved;
            this.interfaceName = _interfaceName;
        }

        /**
         * Get name of the interface created this signal (e.g. org.bluez.Adapter1).
         * @return
         */
        public String getInterfaceName() {
            return interfaceName;
        }

        
        /**
         * Return the changed properties.
         * Key is the properties name, value is Variant containing any type.
         * @return
         */
        public Map<String, Variant<?>> getPropertiesChanged() {
            return propertiesChanged;
        }

        /**
         * Returns a list of removed property keys.        
         * @return
         */
        public List<String> getPropertiesRemoved() {
            return propertiesRemoved;
        }
    }
}
