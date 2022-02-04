package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

@SuppressWarnings("serial")
public class UnknownTypeCodeException extends DBusException implements NonFatalException {
    public UnknownTypeCodeException(byte _code) {
        super("Not a valid D-Bus type code: " + _code);
    }
}
