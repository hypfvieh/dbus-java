package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;

/**
* Messages generated locally in the application.
*/
@DBusInterfaceName("org.freedesktop.DBus.Local")
public interface Local extends DBusInterface {
    class Disconnected extends DBusSignal {
        public Disconnected(String path) throws DBusException {
            super(path);
        }
    }
}