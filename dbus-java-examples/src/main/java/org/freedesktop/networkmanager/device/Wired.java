package org.freedesktop.networkmanager.device;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

@DBusInterfaceName("org.freedesktop.NetworkManager.Device.DeviceEthernet")
public interface Wired extends DBusInterface {
    @SuppressWarnings({"checkstyle:visibilitymodifier", "checkstyle:hideutilityclassconstructor"})
    class PropertiesChanged extends DBusSignal {
        public final Map<CharSequence, Variant<?>> properties;

        public PropertiesChanged(String _path, Map<CharSequence, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }
    }

}
