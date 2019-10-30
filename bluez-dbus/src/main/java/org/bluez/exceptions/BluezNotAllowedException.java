package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotAllowedException extends DBusException {

    public BluezNotAllowedException(String _message) {
        super(_message);
    }

}
