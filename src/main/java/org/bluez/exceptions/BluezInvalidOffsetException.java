package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInvalidOffsetException extends DBusException {

    public BluezInvalidOffsetException(String _message) {
        super(_message);
    }

}
