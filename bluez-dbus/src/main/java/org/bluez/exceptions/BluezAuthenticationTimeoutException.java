package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezAuthenticationTimeoutException extends DBusException {

    public BluezAuthenticationTimeoutException(String _message) {
        super(_message);
    }

}
