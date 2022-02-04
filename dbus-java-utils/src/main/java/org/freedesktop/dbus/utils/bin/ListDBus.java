package org.freedesktop.dbus.utils.bin;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBus;

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

    public static void main(String[] _args) throws Exception {
        boolean owners = false;
        boolean users = false;
        DBusBusType connection = DBusBusType.SESSION;

        for (String a : _args) {
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
                connection = DBusBusType.SESSION;
            } else if ("-s".equals(a)) {
                connection = DBusBusType.SESSION;
            } else if ("--system".equals(a)) {
                connection = DBusBusType.SYSTEM;
            } else if ("-y".equals(a)) {
                connection = DBusBusType.SYSTEM;
            } else {
                syntax();
            }
        }

        DBusConnection conn = DBusConnectionBuilder.forType(connection).build();
        DBus dbus = conn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
        String[] names = dbus.ListNames();
        for (String s : names) {
            if (users) {
                try {
                    System.out.print(dbus.GetConnectionUnixUser(s) + "\t");
                } catch (DBusExecutionException _exDe) {
                    System.out.print("\t");
                }
            }
            System.out.print(s);
            if (!s.startsWith(":") && owners) {
                try {
                    System.out.print("\t" + dbus.GetNameOwner(s));
                } catch (DBusExecutionException _exDe) {
                }
            }
            System.out.println();
        }
        conn.disconnect();
    }
}
