package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a arguments passed to the method are invalid
 */
public class InvalidArgs extends DBusExecutionException {
    public InvalidArgs(String message) {
        super(message);
    }
}
