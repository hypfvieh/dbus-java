package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezConnectFailedException extends DBusException {

    public BluezConnectFailedException(String _message) {
        super(_message);
    }

}
