package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if the object was unknown on a remote connection
 */
public class UnknownObject extends DBusExecutionException {
    public UnknownObject(String message) {
        super(message);
    }
}
