package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a called operation is not supported
 */
public class NotSupported extends DBusExecutionException {
    public NotSupported(String message) {
        super(message);
    }
}
