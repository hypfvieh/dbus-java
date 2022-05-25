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
        // this will never work as the receiving object is of type java.Proxy and not SampleClass / SampleRemoteInterface
        //setFailed(!(s.otherpath instanceof SampleClass), "Must be a SampleRemoteInterface, but was: " + s.otherpath.getClass().getName());
        setFailed(!"This Is A UTF-8 Name: س !!".equals(s.otherpath.getName()), "Name does not match");
    }
}