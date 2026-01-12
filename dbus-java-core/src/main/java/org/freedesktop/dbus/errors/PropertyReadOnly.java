package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.io.Serial;

/**
 * Thrown if a attempt to edit read only property
 */
public class PropertyReadOnly extends DBusExecutionException {
    @Serial
    private static final long serialVersionUID = -8493757965292570003L;

    public PropertyReadOnly(String _message) {
        super(_message);
    }
}
