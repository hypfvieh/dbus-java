package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a attempt to edit read only property
 */
public class PropertyReadOnly extends DBusExecutionException {
    public PropertyReadOnly(String message) {
        super(message);
    }
}
