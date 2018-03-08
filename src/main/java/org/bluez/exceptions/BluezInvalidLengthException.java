package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInvalidLengthException extends DBusException {

    public BluezInvalidLengthException(String _message) {
        super(_message);
    }

}
