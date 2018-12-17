package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.fail;

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
        fail("This signal handler shouldn't be called");
    }
}