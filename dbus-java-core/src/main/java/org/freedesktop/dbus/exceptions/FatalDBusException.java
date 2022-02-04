package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.FatalException;

@SuppressWarnings("serial")
public class FatalDBusException extends DBusException implements FatalException {
    
    public FatalDBusException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public FatalDBusException(Throwable _cause) {
        super(_cause);
    }
    
    public FatalDBusException(String _message) {
        super(_message);
    }
}
