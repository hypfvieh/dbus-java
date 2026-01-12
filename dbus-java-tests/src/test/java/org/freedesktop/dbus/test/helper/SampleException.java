package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.io.Serial;

@IntrospectionDescription("A test exception to throw over DBus")
public class SampleException extends DBusExecutionException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SampleException(String _message) {
        super(_message);
    }
}
