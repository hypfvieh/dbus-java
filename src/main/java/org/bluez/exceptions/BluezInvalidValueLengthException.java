package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInvalidValueLengthException extends DBusException {

    public BluezInvalidValueLengthException(String _message) {
        super(_message);
    }

}
