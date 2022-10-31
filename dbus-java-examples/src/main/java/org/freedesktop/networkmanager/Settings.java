package org.freedesktop.networkmanager;

import java.util.List;
import java.util.Map;

import org.freedesktop.Pair;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.NetworkManager.Settings")
public interface Settings extends DBusInterface, Properties {

    List<DBusInterface> ListConnections();

    DBusInterface GetConnectionByUuid(String _uuid);

    DBusInterface AddConnection(Map<String, Map<String, Variant<?>>> _connection);

    DBusInterface AddConnectionUnsaved(Map<String, Map<String, Variant<?>>> _connection);

    Pair<DBusInterface, Map<String, Variant<?>>> AddConnection2(Map<String, Map<String, Variant<?>>> _settings,
     UInt32 _flags, Map<String, Variant<?>> _args);

    Pair<Boolean, List<String>> LoadConnections(List<String> _filenames);

    boolean ReloadConnections();

    void SaveHostname(String _hostname);

    class NewConnection extends DBusSignal {

        /** Object path of the new connection. */
        private final DBusInterface connection;

        NewConnection(String _path, DBusInterface _connection) throws DBusException {
            super(_path, _connection);
            this.connection = _connection;
        }

        public DBusInterface getConnection() {
            return connection;
        }
    }

    class ConnectionRemoved extends DBusSignal {

        /** Object path of the removed connection. */
        private final DBusInterface connection;

        ConnectionRemoved(String _path, DBusInterface _connection) throws DBusException {
            super(_path, _connection);
            this.connection = _connection;
        }

        public DBusInterface getConnection() {
            return connection;
        }
    }

    class PropertyNames {

        /** List of object paths of available network connection profiles. */
        public static final String Connections = "Connections";
        /** The machine hostname stored in persistent configuration. */
        public static final String Hostname = "Hostname";
        /** If true, adding and modifying connections is supported. */
        public static final String CanModify = "CanModify";
    }
}
