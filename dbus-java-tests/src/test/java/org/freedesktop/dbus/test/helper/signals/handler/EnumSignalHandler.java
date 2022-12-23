package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterfaceEnum.TestEnum;
import org.freedesktop.dbus.test.helper.signals.SampleSignals;

import java.util.Arrays;

/**
 * Untyped signal handler
 */
public class EnumSignalHandler extends AbstractSignalHandler<SampleSignals.TestEnumSignal> {

    public EnumSignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(SampleSignals.TestEnumSignal _t) {
        setFailed(TestEnum.TESTVAL1 != _t.getEnum(), "Invalid enum value: " + _t.getEnum());
        setFailed(!Arrays.asList(TestEnum.TESTVAL2, TestEnum.TESTVAL3).equals(_t.getEnums()), "Invalid enum values: " + _t.getEnums());
    }
}
