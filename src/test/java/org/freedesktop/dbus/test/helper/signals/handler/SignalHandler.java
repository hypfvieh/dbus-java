package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestSignal;
import org.freedesktop.dbus.types.UInt32;

/**
 * Typed signal handler
 */
public class SignalHandler extends SignalHandlerBase<TestSignal> {

    public SignalHandler(int _expectedRuns, UInt32 _expectedIntResult, String _expectedStringResult) {
        super(_expectedRuns, _expectedIntResult, _expectedStringResult);
    }

    @Override
    public void handleImpl(TestSignal t) {
        if (getExpectedIntResult() != null) {
            assertEquals(new UInt32(42), getExpectedIntResult(), "Retrieved int does not match.");
        }
        if (getExpectedStringResult() != null) {
            assertEquals("Bar", getExpectedStringResult(), "Retrieved string does not match.");
        }
    }
}