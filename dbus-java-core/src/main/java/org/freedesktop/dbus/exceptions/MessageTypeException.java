package org.freedesktop.dbus.exceptions;

import java.io.IOException;

import org.freedesktop.dbus.interfaces.NonFatalException;

@SuppressWarnings("serial")
public class MessageTypeException extends IOException implements NonFatalException {
    public MessageTypeException(String _message) {
        super(_message);
    }
}
