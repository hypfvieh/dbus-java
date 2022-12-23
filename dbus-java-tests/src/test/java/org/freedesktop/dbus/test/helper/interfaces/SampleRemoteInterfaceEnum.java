package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.interfaces.DBusInterface;

public interface SampleRemoteInterfaceEnum extends DBusInterface {
    TestEnum getEnumValue();

    enum TestEnum {
        TESTVAL1, TESTVAL2, TESTVAL3;
    }
}
