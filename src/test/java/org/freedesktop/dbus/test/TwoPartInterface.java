/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

public interface TwoPartInterface extends DBusInterface {
    TwoPartObject getNew();
    //CHECKSTYLE:OFF
    class TwoPartSignal extends DBusSignal {
        public final TwoPartObject o;

        public TwoPartSignal(String path, TwoPartObject _o) throws DBusException {
            super(path, _o);
            this.o = _o;
        }
    }
    //CHECKSTYLE:ON
}
