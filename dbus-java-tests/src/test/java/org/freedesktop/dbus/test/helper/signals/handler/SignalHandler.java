package org.freedesktop.dbus.test.helper.signals.handler;

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
    public void handleImpl(TestSignal _t) {
        if (getExpectedIntResult() != null) {
            setFailed(!new UInt32(42).equals(getExpectedIntResult()), "Retrieved int does not match.");
        }
        if (getExpectedStringResult() != null) {
            setFailed(!"Bar".equals(getExpectedStringResult()), "Retrieved string does not match.");
        }
    }
}
