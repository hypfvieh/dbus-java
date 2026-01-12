package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.DBusInterface;

import java.io.Serial;

/**
 * @since 5.1.1 - 2024-11-16
 * @author hypfvieh
 */
public class MissingInterfaceImplementationException extends DBusException {
    @Serial
    private static final long serialVersionUID = 1L;

    public MissingInterfaceImplementationException(Class<?> _clz) {
        super("Given class " + (_clz == null ? null : _clz.getName()) + " does not implement " + DBusInterface.class.getName());
    }

    public MissingInterfaceImplementationException(String _msg) {
        super(_msg);
    }

}
