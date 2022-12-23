package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;

/**
 * Typed signal handler
 */
public abstract class SignalHandlerBase<T extends DBusSignal> extends AbstractSignalHandler<T> {
    private final UInt32 expectedIntResult;
    private final String expectedStringResult;

    public SignalHandlerBase(int _expectedRuns, UInt32 _expectedIntResult, String _expectedStringResult) {
        super(_expectedRuns);
        expectedIntResult = _expectedIntResult;
        expectedStringResult = _expectedStringResult;
    }

    @Override
    public void handleImpl(T _t) {
        if (expectedIntResult != null) {
            assertEquals(new UInt32(42), getExpectedIntResult(), "Retrieved int does not match.");
        }
        if (expectedStringResult != null) {
            assertEquals("Bar", getExpectedStringResult(), "Retrieved string does not match.");
        }
    }

    public UInt32 getExpectedIntResult() {
        return expectedIntResult;
    }

    public String getExpectedStringResult() {
        return expectedStringResult;
    }
}
