package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestEmptySignal;

/**
 * Empty signal handler
 */
public class EmptySignalHandler extends AbstractSignalHandler<TestEmptySignal> {

    public EmptySignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(TestEmptySignal t) {
        System.out.println("EmptySignalHandler called");
    }
}