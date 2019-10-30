package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezAuthenticationFailedException extends DBusException {

    public BluezAuthenticationFailedException(String _message) {
        super(_message);
    }

}
