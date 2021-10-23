package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;

public class InternalSignal extends DBusSignal {
    public InternalSignal(String _source, String _objectpath, String _name, String _iface, String _sig, long _serial, Object... _parameters) throws DBusException {
        super(_source, _objectpath, _iface, _name, _sig, _parameters, _serial);
    }
}
