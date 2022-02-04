package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

@SuppressWarnings("serial")
public class MarshallingException extends DBusException implements NonFatalException {

    public MarshallingException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public MarshallingException(String _message) {
        super(_message);
    }
}
