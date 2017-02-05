package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotSupportedException extends DBusException {

    public BluezNotSupportedException(String _message) {
        super(_message);
    }

}
