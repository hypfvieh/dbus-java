package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.messages.Message;

/**
 * Callback which receives the raw messages seen by a monitor connection.
 * <p>
 * A connection is turned into a monitor connection via
 * {@code DBusConnection.becomeMonitor(...)}. After that, it receives copies of the messages flowing
 * over the bus (as permitted by the given match rules) instead of the connection's normal message
 * handling. Each such message is delivered to this handler as a raw {@link Message}.
 * </p>
 *
 * @since 5.2.1 - 2026-07-18
 */
@FunctionalInterface
public interface DBusMonitorHandler {

    /**
     * Handle a message received on a monitor connection.
     *
     * @param _message the monitored message
     */
    void handle(Message _message);
}
