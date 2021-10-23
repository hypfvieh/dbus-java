package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.test.helper.interfaces.SampleNewInterface;

public class SampleNewInterfaceClass implements SampleNewInterface {
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}