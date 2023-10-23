package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each message type.
 * @since 5.0.0 - 2023-10-23
 */
public interface MessageType {
    byte METHOD_CALL   = 1;
    byte METHOD_RETURN = 2;
    byte ERROR         = 3;
    byte SIGNAL        = 4;
}
