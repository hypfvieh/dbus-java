package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezOutOfRangeException extends DBusException {

    public BluezOutOfRangeException(String _message) {
        super(_message);
    }

}
