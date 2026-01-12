package org.freedesktop.dbus.exceptions;

import java.io.Serial;

public class TransportConfigurationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public TransportConfigurationException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public TransportConfigurationException(String _message) {
        super(_message);
    }

}
