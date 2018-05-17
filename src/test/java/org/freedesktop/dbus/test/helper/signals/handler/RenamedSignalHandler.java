package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestRenamedSignal;
import org.freedesktop.dbus.types.UInt32;

/**
 * Typed signal handler for renamed signal
 */
public class RenamedSignalHandler extends SignalHandlerBase<TestRenamedSignal> {


    public RenamedSignalHandler(int _expectedRuns, UInt32 _expectedIntResult, String _expectedStringResult) {
        super(_expectedRuns, _expectedIntResult, _expectedStringResult);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(TestRenamedSignal t) {
        System.out.println("RenamedSignalHandler called");
    }
}