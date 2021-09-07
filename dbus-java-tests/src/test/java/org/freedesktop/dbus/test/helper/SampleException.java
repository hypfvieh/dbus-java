package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

@IntrospectionDescription("A test exception to throw over DBus")
@SuppressWarnings("serial")
public class SampleException extends DBusExecutionException {
    public SampleException(String message) {
        super(message);
    }
}
