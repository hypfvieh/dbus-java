package org.freedesktop.dbus.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

/**
* A standard properties interface.
*/
@DBusInterfaceName("org.freedesktop.DBus.Properties")
public interface Properties extends DBusInterface {
    /**
     * Get the value for the given property.
     * @param <A> whatever
     * @param interface_name The interface this property is associated with.
     * @param property_name The name of the property.
     * @return The value of the property (may be any valid DBus type).
     */
    <A> A Get(String interface_name, String property_name);

    /**
     * Set the value for the given property.
     * @param <A> whatever
     * @param interface_name The interface this property is associated with.
     * @param property_name The name of the property.
     * @param value The new value of the property (may be any valid DBus type).
     */
    <A> void Set(String interface_name, String property_name, A value);

    /**
     * Get all properties and values.
     * @param interface_name The interface the properties is associated with.
     * @return The properties mapped to their values.
     */
    Map<String, Variant<?>> GetAll(String interface_name);

    /**
     * Signal generated when a property changes.
     */
    public class PropertiesChanged extends DBusSignal {
        public final String interfaceName;
        public final Map<String, Variant<?>> changedProperties;
        public final List<String> invalidatedProperties;

        public PropertiesChanged(final String path, final String _interfaceName,
                final Map<String, Variant<?>> _changedProperties, final List<String> _invalidatedProperties)
                throws DBusException {
            super(path, _interfaceName, _changedProperties, _invalidatedProperties);
            this.interfaceName = _interfaceName;
            this.changedProperties = _changedProperties;
            this.invalidatedProperties = _invalidatedProperties;
        }
    }
}