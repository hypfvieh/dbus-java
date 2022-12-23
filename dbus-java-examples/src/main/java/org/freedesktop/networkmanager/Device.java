package org.freedesktop.networkmanager;

import org.freedesktop.Pair;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

@DBusInterfaceName("org.freedesktop.NetworkManager.Device")
@SuppressWarnings({"checkstyle:methodname", "checkstyle:hideutilityclassconstructor", "checkstyle:visibilitymodifier"})
public interface Device extends DBusInterface {
    void Reapply(Map<CharSequence, Map<CharSequence, Variant<?>>> _connection, UInt64 _versionId, UInt32 _flags);

    Pair<Map<CharSequence, Map<CharSequence, Variant<?>>>, UInt64> GetAppliedConnection(UInt32 _flags);

    void Disconnect();

    void Delete();

    class StateChanged extends DBusSignal {
        public final UInt32 newState;
        public final UInt32 oldState;
        public final UInt32 reason;

        public StateChanged(String _path, UInt32 _newState, UInt32 _oldState, UInt32 _reason) throws DBusException {
            super(_path, _newState, _oldState, _reason);
            this.newState = _newState;
            this.oldState = _oldState;
            this.reason = _reason;
        }
    }

}
