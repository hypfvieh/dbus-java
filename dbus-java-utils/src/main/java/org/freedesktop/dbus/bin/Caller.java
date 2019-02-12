/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.bin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;

public final class Caller {

    private Caller() {

    }

    public static void main(String[] args) {
        String addr = System.getenv("DBUS_SESSION_BUS_ADDRESS");

        try (AbstractTransport conn = TransportFactory.createTransport(new BusAddress(addr))) {
            if (args.length < 4) {
                System.out.println("Syntax: Caller <dest> <path> <interface> <method> [<sig> <args>]");
                System.exit(1);
            }

            Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
            conn.writeMessage(m);

            if ("".equals(args[2])) {
                args[2] = null;
            }
            if (args.length == 4) {
                m = new MethodCall(args[0], args[1], args[2], args[3], (byte) 0, null);
            } else {
                List<Type> lts = new ArrayList<>();
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
            conn.writeMessage(m);
            do {
                m = conn.readMessage();
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
