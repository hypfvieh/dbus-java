package org.freedesktop.dbus.test.helper.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.test.helper.structs.ProfileStruct;

public interface Profiler extends DBusInterface {
    class ProfileSignal extends DBusSignal {
        public ProfileSignal(String path) throws DBusException {
            super(path);
        }
    }

    void array(int[] v);

    void stringarray(String[] v);

    void map(Map<String, String> m);

    void list(List<String> l);

    void bytes(byte[] b);

    void struct(ProfileStruct ps);

    void string(String s);

    //CHECKSTYLE:OFF
    void NoReply();

    void Pong();
    //CHECKSTYLE:ON
}
