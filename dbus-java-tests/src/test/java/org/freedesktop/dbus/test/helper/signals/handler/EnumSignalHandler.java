package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterfaceEnum.TestEnum;
import org.freedesktop.dbus.test.helper.signals.SampleSignals;

/**
 * Untyped signal handler
 */
public class EnumSignalHandler extends AbstractSignalHandler<SampleSignals.TestEnumSignal> {

    public EnumSignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(SampleSignals.TestEnumSignal t) {
    	assertEquals(TestEnum.TESTVAL1, t.getEnum());
    	assertEquals(Arrays.asList(TestEnum.TESTVAL2, TestEnum.TESTVAL3), t.getEnums());
    }
}