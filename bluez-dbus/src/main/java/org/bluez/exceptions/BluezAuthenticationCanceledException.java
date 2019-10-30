package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezAuthenticationCanceledException extends DBusException {

    public BluezAuthenticationCanceledException(String _message) {
        super(_message);
    }

}
