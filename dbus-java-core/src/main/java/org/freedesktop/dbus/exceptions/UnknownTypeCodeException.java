package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

import java.io.Serial;

public class UnknownTypeCodeException extends DBusException implements NonFatalException {
    @Serial
    private static final long serialVersionUID = -4688075573912580455L;

    public UnknownTypeCodeException(byte _code) {
        super("Not a valid D-Bus type code: " + _code);
    }
}
