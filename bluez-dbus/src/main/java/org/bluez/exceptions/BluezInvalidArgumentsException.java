package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInvalidArgumentsException extends DBusException {

    public BluezInvalidArgumentsException(String _message) {
        super(_message);
    }

}
