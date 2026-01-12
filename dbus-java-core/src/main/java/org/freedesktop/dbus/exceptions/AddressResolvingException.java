package org.freedesktop.dbus.exceptions;

import java.io.Serial;

public class AddressResolvingException extends DBusExecutionException {

    @Serial
    private static final long serialVersionUID = -1636993356304776163L;

    public AddressResolvingException(String _message) {
        super(_message);
    }

}
