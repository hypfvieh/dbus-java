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
    }

    @Override
    public void stringarray(String[] _v) {
    }

    @Override
    public void map(Map<String, String> _m) {
    }

    @Override
    public void list(List<String> _l) {
    }

    @Override
    public void bytes(byte[] _b) {
    }

    @Override
    public void struct(ProfileStruct _ps) {
    }

    @Override
    public void string(String _s) {
    }

    @Override
    public void NoReply() {
    }

    @Override
    public void Pong() {
    }
}
