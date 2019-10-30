package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezFailedException extends DBusException {

    public BluezFailedException(String _message) {
        super(_message);
    }

}
