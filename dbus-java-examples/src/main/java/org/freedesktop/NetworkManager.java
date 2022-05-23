package org.freedesktop;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.NetworkManager")
public interface NetworkManager extends DBusInterface, Properties {
    class CheckPermissions extends DBusSignal {
        public CheckPermissions(String _path) throws DBusException {
            super(_path);
        }
    }

    class StateChanged extends DBusSignal {
        public final UInt32 state;

        public StateChanged(String _path, UInt32 _state) throws DBusException {
            super(_path, _state);
            this.state = _state;
        }
    }

    class DeviceAdded extends DBusSignal {
        public final DBusInterface devicePath;

        public DeviceAdded(String _path, DBusInterface _devicePath) throws DBusException {
            super(_path, _devicePath);
            this.devicePath = _devicePath;
        }
    }

    class DeviceRemoved extends DBusSignal {
        public final DBusInterface devicePath;

        public DeviceRemoved(String _path, DBusInterface _devicePath) throws DBusException {
            super(_path, _devicePath);
            this.devicePath = _devicePath;
        }
    }

   void Reload(UInt32 _flags);

   List<DBusPath> GetDevices();

   List<DBusPath> GetAllDevices();

   DBusInterface GetDeviceByIpIface(CharSequence _iface);

   DBusInterface ActivateConnection(DBusInterface _connection, DBusInterface _device,
    DBusInterface _specificObject);

   Pair<DBusInterface, DBusInterface> AddAndActivateConnection(
    Map<CharSequence, Map<CharSequence, Variant<?>>> _connection, DBusInterface _device,
    DBusInterface _specificObject);

   void DeactivateConnection(DBusInterface _activeConnection);

   void Sleep(boolean _sleep);

   void Enable(boolean _enable);

   Map<CharSequence, CharSequence> GetPermissions();

   void SetLogging(CharSequence _level, CharSequence _domains);

   Pair<CharSequence, CharSequence> GetLogging();

   UInt32 CheckConnectivity();

   UInt32 state();

   DBusInterface CheckpointCreate(List<DBusInterface> _devices, UInt32 _rollbackTimeout, UInt32 _flags);

   void CheckpointDestroy(DBusInterface _checkpoint);

   Map<CharSequence, UInt32> CheckpointRollback(DBusInterface _checkpoint);

   void CheckpointAdjustRollbackTimeout(DBusInterface _checkpoint, UInt32 _addTimeout);
}
