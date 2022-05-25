package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.messages.DBusSignal;

/**
 * handler which should never be called
 */
public class BadArraySignalHandler<T extends DBusSignal> extends AbstractSignalHandler<T> {

    public BadArraySignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(T s) {
        setFailed(false, "This signal handler shouldn't be called");
    }
}