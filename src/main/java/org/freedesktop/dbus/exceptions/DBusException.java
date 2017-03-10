/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.exceptions;

/**
 * An exception within DBus.
 */
public class DBusException extends Exception {
    private static final long serialVersionUID = -1L;

    /**
    * Create an exception with the specified message
    * @param message message
    */
    public DBusException(String message) {
        super(message);
    }

    public DBusException() {
        super();
    }

    public DBusException(String _message, Throwable _cause, boolean _enableSuppression, boolean _writableStackTrace) {
        super(_message, _cause, _enableSuppression, _writableStackTrace);
    }

    public DBusException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public DBusException(Throwable _cause) {
        super(_cause);
    }
}
