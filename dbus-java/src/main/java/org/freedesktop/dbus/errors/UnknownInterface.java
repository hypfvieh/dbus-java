package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a interface does not exist
 */
public class UnknownInterface extends DBusExecutionException {
    public UnknownInterface(String message) {
        super(message);
    }
}
