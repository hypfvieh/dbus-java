package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a message is denied due to a security policy
 */
public class AccessDenied extends DBusExecutionException {
    public AccessDenied(String message) {
        super(message);
    }
}
