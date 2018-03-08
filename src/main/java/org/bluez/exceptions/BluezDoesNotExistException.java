package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezDoesNotExistException extends DBusException {

    public BluezDoesNotExistException(String _message) {
        super(_message);
    }

}
