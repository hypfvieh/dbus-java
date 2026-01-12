package org.freedesktop.dbus.exceptions;

import java.io.Serial;
import java.util.Collection;

/**
 * @since 5.1.0 - 2024-03-18
 * @author hypfvieh
 */
public class InvalidInterfaceSignature extends DBusException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidInterfaceSignature(Collection<String> _invalid) {
        super("Interfaces on exported objects must be public. Non-public: " + String.join(", ", _invalid));
    }

}
