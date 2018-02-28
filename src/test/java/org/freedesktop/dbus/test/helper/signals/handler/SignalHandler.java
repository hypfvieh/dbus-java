package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestSignal;
import org.freedesktop.dbus.types.UInt32;
import org.junit.Assert;

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
            Assert.assertEquals("Retrieved int does not match.", new UInt32(42), getExpectedIntResult());
        }
        if (getExpectedStringResult() != null) {
            Assert.assertEquals("Retrieved string does not match.","Bar", getExpectedStringResult());
        }
    }
}