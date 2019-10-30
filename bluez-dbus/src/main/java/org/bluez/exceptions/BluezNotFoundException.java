package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotFoundException extends DBusException {

    public BluezNotFoundException(String _message) {
        super(_message);
    }

}
