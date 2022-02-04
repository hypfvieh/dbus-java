package org.freedesktop.dbus.exceptions;

import java.io.IOException;

import org.freedesktop.dbus.interfaces.FatalException;

@SuppressWarnings("serial")
public class MessageProtocolVersionException extends IOException implements FatalException {
    public MessageProtocolVersionException(String _message) {
        super(_message);
    }
}
