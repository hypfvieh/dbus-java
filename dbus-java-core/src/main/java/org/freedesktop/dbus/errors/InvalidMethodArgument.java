package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.io.Serial;

/**
 * Thrown if a arguments passed to the method are invalid
 */
public class InvalidMethodArgument extends DBusExecutionException {
    @Serial
    private static final long serialVersionUID = 2504012938615867394L;

    public InvalidMethodArgument(String _message) {
        super(_message);
    }
}
