package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestObjectSignal;

/**
 * Object path signal handler
 */
public class ObjectSignalHandler extends AbstractSignalHandler<TestObjectSignal> {

    public ObjectSignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    @Override
    public void handleImpl(TestObjectSignal s) {
        System.out.println(s.otherpath);
    }
}