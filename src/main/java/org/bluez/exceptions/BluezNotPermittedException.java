package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotPermittedException extends DBusException {

    public BluezNotPermittedException(String _message) {
        super(_message);
    }

}
