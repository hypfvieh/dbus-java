package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInProgressException extends DBusException {

    public BluezInProgressException(String _message) {
        super(_message);
    }

}
