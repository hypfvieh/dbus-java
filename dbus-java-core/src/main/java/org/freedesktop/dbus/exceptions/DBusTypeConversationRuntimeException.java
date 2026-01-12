package org.freedesktop.dbus.exceptions;

import java.io.Serial;

/**
 * Exception thrown when type conversation failed.
 *
 * @author hypfvieh
 * @since 5.1.0 - 2024-05-19
 */
public class DBusTypeConversationRuntimeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DBusTypeConversationRuntimeException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public DBusTypeConversationRuntimeException(String _message) {
        super(_message);
    }

}
