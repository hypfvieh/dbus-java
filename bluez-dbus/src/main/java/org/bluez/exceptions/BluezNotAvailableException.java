package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotAvailableException extends DBusException {

    public BluezNotAvailableException(String _message) {
        super(_message);
    }

}
