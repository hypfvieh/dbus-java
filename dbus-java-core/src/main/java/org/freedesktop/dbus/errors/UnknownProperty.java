package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.io.Serial;

/**
 * Thrown if a property does not exist in the interface
 */
public class UnknownProperty extends DBusExecutionException {
    @Serial
    private static final long serialVersionUID = 7993712944238574483L;

    public UnknownProperty(String _message) {
        super(_message);
    }
}
