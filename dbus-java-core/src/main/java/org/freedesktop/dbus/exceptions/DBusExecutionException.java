package org.freedesktop.dbus.exceptions;

import java.util.Objects;

/**
 * An exception while running a remote method within DBus.
 */
@SuppressWarnings("checkstyle:mutableexception")
public class DBusExecutionException extends RuntimeException {
    private static final long serialVersionUID = 6327661667731344250L;

    private String type;

    /**
    * Create an exception with the specified message
    * @param _message message
    */
    public DBusExecutionException(String _message) {
        super(_message);
    }

    /**
    * Create an exception with the specified message
    * @param _message message
    * @param _cause cause
    */
    public DBusExecutionException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public void setType(String _type) {
        this.type = _type;
    }

    /**
    * Get the DBus type of this exception. Use if this
    * was an exception we don't have a class file for.
    *
    * @return string
    */
    public String getType() {
        return Objects.requireNonNullElseGet(type, () -> getClass().getName());
    }
}
