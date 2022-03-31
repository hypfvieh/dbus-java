package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.freedesktop.dbus.test.helper.SampleClass;
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
        assertTrue(s.otherpath instanceof SampleClass, "Must be a SampleRemoteInterface");
        assertEquals(((SampleClass) s.otherpath).getName(), "This Is A UTF-8 Name: س !!");
    }
}