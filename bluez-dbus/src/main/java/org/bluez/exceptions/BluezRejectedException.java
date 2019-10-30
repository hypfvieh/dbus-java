package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezRejectedException extends DBusException {

    public BluezRejectedException(String _message) {
        super(_message);
    }

}
