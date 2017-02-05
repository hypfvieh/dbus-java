package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotReadyException extends DBusException {

    public BluezNotReadyException(String _message) {
        super(_message);
    }

}
