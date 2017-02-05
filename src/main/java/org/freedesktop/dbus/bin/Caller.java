/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Vector;

import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.Error;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.Transport;

public class Caller {

    public static void main(String[] args) {
        try {
            if (args.length < 4) {
                System.out.println("Syntax: Caller <dest> <path> <interface> <method> [<sig> <args>]");
                System.exit(1);
            }
            String addr = System.getenv("DBUS_SESSION_BUS_ADDRESS");
            BusAddress address = new BusAddress(addr);
            Transport conn = new Transport(address);

            Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
            ;
            conn.mout.writeMessage(m);

            if ("".equals(args[2])) {
                args[2] = null;
            }
            if (args.length == 4) {
                m = new MethodCall(args[0], args[1], args[2], args[3], (byte) 0, null);
            } else {
                Vector<Type> lts = new Vector<Type>();
                Marshalling.getJavaType(args[4], lts, -1);
                Type[] ts = lts.toArray(new Type[0]);
                Object[] os = new Object[args.length - 5];
                for (int i = 5; i < args.length; i++) {
                    if (ts[i - 5] instanceof Class) {
                        try {
                            Constructor<?> c = ((Class<?>) ts[i - 5]).getConstructor(String.class);
                            os[i - 5] = c.newInstance(args[i]);
                        } catch (Exception e) {
                            os[i - 5] = args[i];
                        }
                    } else {
                        os[i - 5] = args[i];
                    }
                }
                m = new MethodCall(args[0], args[1], args[2], args[3], (byte) 0, args[4], os);
            }
            long serial = m.getSerial();
            conn.mout.writeMessage(m);
            do {
                m = conn.min.readMessage();
            } while (serial != m.getReplySerial());
            if (m instanceof Error) {
                ((Error) m).throwException();
            } else {
                Object[] os = m.getParameters();
                System.out.println(Arrays.deepToString(os));
            }
        } catch (Exception e) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
