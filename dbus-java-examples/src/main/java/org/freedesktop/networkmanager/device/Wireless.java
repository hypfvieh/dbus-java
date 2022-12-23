package org.freedesktop.networkmanager.device;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.Wireless")
@SuppressWarnings({"checkstyle:methodname", "checkstyle:hideutilityclassconstructor"})
public interface Wireless extends DBusInterface {

    List<DBusPath> GetAccessPoints();

    List<DBusPath> GetAllAccessPoints();

    void RequestScan(Map<String, Variant<?>> _options);

    class PropertiesChanged extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        PropertiesChanged(String _path, String _interfaceName, Map<String, Variant<?>> _properties)
                throws DBusException {
            super(_path, _interfaceName);
            this.properties = _properties;
        }

        public Map<String, Variant<?>> getProperties() {
            return properties;
        }

    }

    class AccessPointAdded extends DBusSignal {

        private final DBusPath accessPoint;

        AccessPointAdded(String _path, String _interfaceName, DBusPath _accessPoint) throws DBusException {
            super(_path, _interfaceName);
            this.accessPoint = _accessPoint;
        }

        public DBusPath getAccessPoint() {
            return accessPoint;
        }

    }

    class AccessPointRemoved extends DBusSignal {

        private final DBusPath accessPoint;

        AccessPointRemoved(String _path, String _interfaceName, DBusPath _accessPoint) throws DBusException {
            super(_path, _interfaceName);
            this.accessPoint = _accessPoint;
        }

        public DBusPath getAccessPoint() {
            return accessPoint;
        }

    }
}
