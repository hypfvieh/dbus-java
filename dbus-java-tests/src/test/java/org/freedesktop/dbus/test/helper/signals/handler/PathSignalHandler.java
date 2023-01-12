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
    public void handleImpl(TestPathSignal _t) {
        logger.debug("Path sighandler: " + _t);
    }
}
