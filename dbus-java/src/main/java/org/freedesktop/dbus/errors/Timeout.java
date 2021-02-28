package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a operation timed out
 */
public class Timeout extends DBusExecutionException {
    public Timeout(String message) {
        super(message);
    }
}
