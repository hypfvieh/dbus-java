package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezAlreadyConnectedException extends DBusException {

    public BluezAlreadyConnectedException(String _message) {
        super(_message);
    }

}
