package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezHealthErrorException extends DBusException {

    public BluezHealthErrorException(String _message) {
        super(_message);
    }

}
