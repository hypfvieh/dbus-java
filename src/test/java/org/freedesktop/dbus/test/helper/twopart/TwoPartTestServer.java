/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test.helper.twopart;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartInterface;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartObject;

public class TwoPartTestServer implements TwoPartInterface, DBusSigHandler<TwoPartInterface.TwoPartSignal> {
    public class TwoPartTestObject implements TwoPartObject {
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
            return "give name";
        }
    }

    private DBusConnection conn;

    public TwoPartTestServer(DBusConnection _conn) {
        this.conn = _conn;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public TwoPartObject getNew() {
        TwoPartObject o = new TwoPartTestObject();
        System.out.println("export new");
        try {
            conn.exportObject("/12345", o);
        } catch (Exception e) {
        }
        System.out.println("give new");
        return o;
    }

    @Override
    public void handle(TwoPartInterface.TwoPartSignal s) {
        System.out.println("Got: " + s.o);
    }
    
}
