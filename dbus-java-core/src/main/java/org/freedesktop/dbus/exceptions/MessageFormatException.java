package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

import java.io.Serial;

/**
 * Thrown if a message is formatted incorrectly.
 */
public class MessageFormatException extends DBusException implements NonFatalException {
    @Serial
    private static final long serialVersionUID = -4806500517504320924L;

    public MessageFormatException(String _message) {
        super(_message);
    }
}
