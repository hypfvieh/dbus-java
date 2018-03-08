package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotInProgressException extends DBusException {

    public BluezNotInProgressException(String _message) {
        super(_message);
    }

}
