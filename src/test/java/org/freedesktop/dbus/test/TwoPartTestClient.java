/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusConnection;

public final class TwoPartTestClient {

    private TwoPartTestClient() {

    }

    public static class TwoPartTestObject implements TwoPartObject {
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
            System.out.println("client name");
            return toString();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("get conn");
        DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
        System.out.println("get remote");
        TwoPartInterface remote = conn.getRemoteObject("org.freedesktop.dbus.test.two_part_server", "/", TwoPartInterface.class);
        System.out.println("get object");
        TwoPartObject o = remote.getNew();
        System.out.println("get name");
        System.out.println(o.getName());
        TwoPartTestObject tpto = new TwoPartTestObject();
        conn.exportObject("/TestObject", tpto);
        conn.sendSignal(new TwoPartInterface.TwoPartSignal("/FromObject", tpto));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        conn.disconnect();
    }
}
