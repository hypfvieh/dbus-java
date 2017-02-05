package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezDoesNotExistsException extends DBusException {

    public BluezDoesNotExistsException(String _message) {
        super(_message);
    }

}
