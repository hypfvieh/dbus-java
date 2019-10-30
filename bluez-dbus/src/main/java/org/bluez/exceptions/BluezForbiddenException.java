package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezForbiddenException extends DBusException {

    public BluezForbiddenException(String _message) {
        super(_message);
    }

}
