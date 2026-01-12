package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.io.Serial;

/**
 * Thrown if a message is denied due to a security policy
 */
public class AccessDenied extends DBusExecutionException {
    @Serial
    private static final long serialVersionUID = 368173196466740803L;

    public AccessDenied(String _message) {
        super(_message);
    }
}
