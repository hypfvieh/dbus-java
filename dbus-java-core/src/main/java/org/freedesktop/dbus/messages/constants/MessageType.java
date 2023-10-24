package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each message type.
 * @since 5.0.0 - 2023-10-23
 */
public final class MessageType {
    public static final byte METHOD_CALL   = 1;
    public static final byte METHOD_RETURN = 2;
    public static final byte ERROR         = 3;
    public static final byte SIGNAL        = 4;

    private MessageType() {

    }
}
