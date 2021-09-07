package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestPathSignal;

/**
 * Typed signal handler
 */
public class PathSignalHandler extends AbstractSignalHandler<TestPathSignal> {

    public PathSignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(TestPathSignal t) {
        System.out.println("Path sighandler: " + t);
    }
}