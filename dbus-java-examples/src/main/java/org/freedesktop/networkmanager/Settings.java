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

    public List<DBusInterface> ListConnections();

    public DBusInterface GetConnectionByUuid(String uuid);

    public DBusInterface AddConnection(Map<String, Map<String, Variant<?>>> connection);

    public DBusInterface AddConnectionUnsaved(Map<String, Map<String, Variant<?>>> connection);

    public Pair<DBusInterface, Map<String, Variant<?>>> AddConnection2(Map<String, Map<String, Variant<?>>> settings,
            UInt32 flags, Map<String, Variant<?>> args);

    public Pair<Boolean, List<String>> LoadConnections(List<String> filenames);

    public boolean ReloadConnections();

    public void SaveHostname(String hostname);

    public static class NewConnection extends DBusSignal {

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

    public static class ConnectionRemoved extends DBusSignal {

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
