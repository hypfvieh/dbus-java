package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotImplementedException extends DBusException {

    public BluezNotImplementedException(String _message) {
        super(_message);
    }

}
