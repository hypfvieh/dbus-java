package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if the requested service was not available
 */
public class ServiceUnknown extends DBusExecutionException {
    public ServiceUnknown(String message) {
        super(message);
    }
}
