package org.freedesktop.dbus.test.helper.signals.handler;

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
        setFailed(TestEnum.TESTVAL1 != t.getEnum(), "Invalid enum value: " + t.getEnum());
        setFailed(!Arrays.asList(TestEnum.TESTVAL2, TestEnum.TESTVAL3).equals(t.getEnums()), "Invalid enum values: " + t.getEnums());
    }
}