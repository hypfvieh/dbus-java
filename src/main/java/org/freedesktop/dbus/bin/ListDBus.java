/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * This class lists all the names currently connected on the bus
 */
public final class ListDBus {
    public static void syntax() {
        System.out.println("Syntax: ListDBus [--version] [-v] [--help] [-h] [--owners] [-o] [--uids] [-u] [--session] [-s] [--system] [-y]");
        System.exit(1);
    }

    private ListDBus() {

    }

    public static void version() {
        System.out.println("Java D-Bus Version " + System.getProperty("Version"));
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        boolean owners = false;
        boolean users = false;
        int connection = DBusConnection.SESSION;

        for (String a : args) {
            if ("--help".equals(a)) {
                syntax();
            } else if ("-h".equals(a)) {
                syntax();
            } else if ("--version".equals(a)) {
                version();
            } else if ("-v".equals(a)) {
                version();
            } else if ("-u".equals(a)) {
                users = true;
            } else if ("--uids".equals(a)) {
                users = true;
            } else if ("-o".equals(a)) {
                owners = true;
            } else if ("--owners".equals(a)) {
                owners = true;
            } else if ("--session".equals(a)) {
                connection = DBusConnection.SESSION;
            } else if ("-s".equals(a)) {
                connection = DBusConnection.SESSION;
            } else if ("--system".equals(a)) {
                connection = DBusConnection.SYSTEM;
            } else if ("-y".equals(a)) {
                connection = DBusConnection.SYSTEM;
            } else {
                syntax();
            }
        }

        DBusConnection conn = DBusConnection.getConnection(connection);
        DBus dbus = conn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
        String[] names = dbus.ListNames();
        for (String s : names) {
            if (users) {
                try {
                    System.out.print(dbus.GetConnectionUnixUser(s) + "\t");
                } catch (DBusExecutionException exDe) {
                    System.out.print("\t");
                }
            }
            System.out.print(s);
            if (!s.startsWith(":") && owners) {
                try {
                    System.out.print("\t" + dbus.GetNameOwner(s));
                } catch (DBusExecutionException exDe) {
                }
            }
            System.out.println();
        }
        conn.disconnect();
    }
}
