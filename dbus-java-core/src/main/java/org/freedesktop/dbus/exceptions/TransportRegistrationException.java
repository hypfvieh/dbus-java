package org.freedesktop.dbus.exceptions;

import java.io.Serial;

/**
 * Thrown if registration of transport providers fails.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-08
 */
public class TransportRegistrationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public TransportRegistrationException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public TransportRegistrationException(String _message) {
        super(_message);
    }

}
