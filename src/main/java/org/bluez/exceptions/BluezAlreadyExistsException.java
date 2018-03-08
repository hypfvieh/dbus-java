package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezAlreadyExistsException extends DBusException {

    public BluezAlreadyExistsException(String _message) {
        super(_message);
    }

}
