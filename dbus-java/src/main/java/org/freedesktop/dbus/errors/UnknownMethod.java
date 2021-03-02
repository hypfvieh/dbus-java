package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if the method called was unknown on the remote object
 */
public class UnknownMethod extends DBusExecutionException {
    public UnknownMethod(String message) {
        super(message);
    }
}
