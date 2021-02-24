package org.freedesktop.dbus.test;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.test.helper.interfaces.Profiler;
import org.freedesktop.dbus.test.helper.structs.ProfileStruct;

public class ProfilerInstance implements Profiler {
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public void array(int[] v) {
        return;
    }

    @Override
    public void stringarray(String[] v) {
        return;
    }

    @Override
    public void map(Map<String, String> m) {
        return;
    }

    @Override
    public void list(List<String> l) {
        return;
    }

    @Override
    public void bytes(byte[] b) {
        return;
    }

    @Override
    public void struct(ProfileStruct ps) {
        return;
    }

    @Override
    public void string(String s) {
        return;
    }

    @Override
    public void NoReply() {
        return;
    }

    @Override
    public void Pong() {
        return;
    }
}
