package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a property does not exist in the interface
 */
public class UnknownProperty extends DBusExecutionException {
    public UnknownProperty(String message) {
        super(message);
    }
}
