package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInvalidArgumentException extends DBusException {

    public BluezInvalidArgumentException(String _message) {
        super(_message);
    }

}
