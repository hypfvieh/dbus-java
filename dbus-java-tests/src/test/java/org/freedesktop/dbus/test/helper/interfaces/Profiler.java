package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.test.helper.structs.ProfileStruct;

import java.util.List;
import java.util.Map;

public interface Profiler extends DBusInterface {
    void array(int[] _v);

    void stringarray(String[] _v);

    void map(Map<String, String> _m);

    void list(List<String> _l);

    void bytes(byte[] _b);

    void struct(ProfileStruct _ps);

    void string(String _s);

    //CHECKSTYLE:OFF
    void NoReply();

    void Pong();
    //CHECKSTYLE:ON

    class ProfileSignal extends DBusSignal {
        public ProfileSignal(String _path) throws DBusException {
            super(_path);
        }
    }
}
