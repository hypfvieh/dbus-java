package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezCanceledException extends DBusException {

    public BluezCanceledException(String _message) {
        super(_message);
    }

}
