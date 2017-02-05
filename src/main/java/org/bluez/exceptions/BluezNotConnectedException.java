package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotConnectedException extends DBusException {

    public BluezNotConnectedException(String _message) {
        super(_message);
    }

}
