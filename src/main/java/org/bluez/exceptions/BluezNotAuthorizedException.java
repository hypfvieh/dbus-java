package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezNotAuthorizedException extends DBusException {

    public BluezNotAuthorizedException(String _message) {
        super(_message);
    }

}
