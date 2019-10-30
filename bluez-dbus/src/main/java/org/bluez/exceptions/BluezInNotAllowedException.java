package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezInNotAllowedException extends DBusException {

    public BluezInNotAllowedException(String _message) {
        super(_message);
    }

}
