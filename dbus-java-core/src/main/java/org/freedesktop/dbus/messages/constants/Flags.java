package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants representing the flags which can be set on a message.
 * @since 5.0.0 - 2023-10-23
 */
public interface Flags {
    byte NO_REPLY_EXPECTED = 0x01;
    byte NO_AUTO_START     = 0x02;
    byte ASYNC             = 0x40;
}
