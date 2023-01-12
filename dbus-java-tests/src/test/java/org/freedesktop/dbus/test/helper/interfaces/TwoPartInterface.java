package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

public interface TwoPartInterface extends DBusInterface {
    TwoPartObject getNew();
    //CHECKSTYLE:OFF
    class TwoPartSignal extends DBusSignal {
        public final TwoPartObject o;

        public TwoPartSignal(String _path, TwoPartObject _o) throws DBusException {
            super(_path, _o);
            this.o = _o;
        }
    }
    //CHECKSTYLE:ON
}
