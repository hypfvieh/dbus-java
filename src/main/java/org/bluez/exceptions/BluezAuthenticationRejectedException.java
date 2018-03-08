package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezAuthenticationRejectedException extends DBusException {

    public BluezAuthenticationRejectedException(String _message) {
        super(_message);
    }

}
