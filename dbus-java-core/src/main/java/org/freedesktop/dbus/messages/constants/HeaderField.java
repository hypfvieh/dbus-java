package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each valid header field type.
 * @since 5.0.0 - 2023-10-23
 */
public interface HeaderField {
    int MAX_FIELDS    = 10;

    byte PATH         = 1;
    byte INTERFACE    = 2;
    byte MEMBER       = 3;
    byte ERROR_NAME   = 4;
    byte REPLY_SERIAL = 5;
    byte DESTINATION  = 6;
    byte SENDER       = 7;
    byte SIGNATURE    = 8;
    byte UNIX_FDS     = 9;
}
