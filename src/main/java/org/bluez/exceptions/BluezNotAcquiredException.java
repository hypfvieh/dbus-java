package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotAcquiredException extends DBusException {

    public BluezNotAcquiredException(String _message) {
        super(_message);
    }

}
