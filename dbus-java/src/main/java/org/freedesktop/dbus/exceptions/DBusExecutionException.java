package org.freedesktop.dbus.exceptions;

/**
 * An exception while running a remote method within DBus.
 */
@SuppressWarnings("serial")
public class DBusExecutionException extends RuntimeException {
    private String type;

    /**
    * Create an exception with the specified message
    * @param message message
    */
    public DBusExecutionException(String message) {
        super(message);
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
        if (null == type)
            return getClass().getName();
        else
            return type;
    }
}
