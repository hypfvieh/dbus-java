package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

import java.io.Serial;

public class InternalMessageException extends DBusExecutionException implements NonFatalException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InternalMessageException(String _message) {
        super(_message);
    }
}
