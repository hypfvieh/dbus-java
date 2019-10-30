package org.bluez.exceptions;

import org.freedesktop.dbus.exceptions.DBusException;

@SuppressWarnings("serial")
public class BluezConnectionAttemptFailedException extends DBusException {

    public BluezConnectionAttemptFailedException(String _message) {
        super(_message);
    }

}
