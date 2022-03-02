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
    public static class CheckPermissions extends DBusSignal {
        public CheckPermissions(String path) throws DBusException {
            super(path);
        }
    }

    public static class StateChanged extends DBusSignal {
        public final UInt32 state;

        public StateChanged(String path, UInt32 state) throws DBusException {
            super(path, state);
            this.state = state;
        }
    }

    public static class DeviceAdded extends DBusSignal {
        public final DBusInterface device_path;

        public DeviceAdded(String path, DBusInterface device_path) throws DBusException {
            super(path, device_path);
            this.device_path = device_path;
        }
    }

    public static class DeviceRemoved extends DBusSignal {
        public final DBusInterface device_path;

        public DeviceRemoved(String path, DBusInterface device_path) throws DBusException {
            super(path, device_path);
            this.device_path = device_path;
        }
    }

    public void Reload(UInt32 flags);

    public List<DBusPath> GetDevices();

    public List<DBusPath> GetAllDevices();

    public DBusInterface GetDeviceByIpIface(CharSequence iface);

    public DBusInterface ActivateConnection(DBusInterface connection, DBusInterface device,
            DBusInterface specific_object);

    public Pair<DBusInterface, DBusInterface> AddAndActivateConnection(
            Map<CharSequence, Map<CharSequence, Variant<?>>> connection, DBusInterface device,
            DBusInterface specific_object);

    public void DeactivateConnection(DBusInterface active_connection);

    public void Sleep(boolean sleep);

    public void Enable(boolean enable);

    public Map<CharSequence, CharSequence> GetPermissions();

    public void SetLogging(CharSequence level, CharSequence domains);

    public Pair<CharSequence, CharSequence> GetLogging();

    public UInt32 CheckConnectivity();

    public UInt32 state();

    public DBusInterface CheckpointCreate(List<DBusInterface> devices, UInt32 rollback_timeout, UInt32 flags);

    public void CheckpointDestroy(DBusInterface checkpoint);

    public Map<CharSequence, UInt32> CheckpointRollback(DBusInterface checkpoint);

    public void CheckpointAdjustRollbackTimeout(DBusInterface checkpoint, UInt32 add_timeout);
}
