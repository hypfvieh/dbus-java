package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.test.helper.interfaces.Profiler;
import org.freedesktop.dbus.test.helper.structs.ProfileStruct;

import java.util.List;
import java.util.Map;

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
    public void array(int[] _v) {
        return;
    }

    @Override
    public void stringarray(String[] _v) {
        return;
    }

    @Override
    public void map(Map<String, String> _m) {
        return;
    }

    @Override
    public void list(List<String> _l) {
        return;
    }

    @Override
    public void bytes(byte[] _b) {
        return;
    }

    @Override
    public void struct(ProfileStruct _ps) {
        return;
    }

    @Override
    public void string(String _s) {
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
