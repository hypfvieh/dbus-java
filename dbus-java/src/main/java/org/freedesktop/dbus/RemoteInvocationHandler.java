/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.MethodNoReply;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.NoReply;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteInvocationHandler implements InvocationHandler {
    public static final int CALL_TYPE_SYNC     = 0;
    public static final int CALL_TYPE_ASYNC    = 1;
    public static final int CALL_TYPE_CALLBACK = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteInvocationHandler.class);



    public static Object convertRV(String sig, Object[] rp, Method m, AbstractConnection conn) throws DBusException {
        Class<? extends Object> c = m.getReturnType();

        if (null == rp) {
            if (null == c || Void.TYPE.equals(c)) {
                return null;
            } else {
                throw new DBusException("Wrong return type (got void, expected a value)");
            }
        } else {
            try {
                LOGGER.trace("Converting return parameters from {} to type {}",Arrays.deepToString(rp), m.getGenericReturnType());
                rp = Marshalling.deSerializeParameters(rp, new Type[] {
                        m.getGenericReturnType()
                }, conn);
            } catch (Exception e) {
                LOGGER.debug("Wrong return type.", e);
                throw new DBusException(String.format("Wrong return type (failed to de-serialize correct types: %s )", e.getMessage()));
            }
        }

        switch (rp.length) {
        case 0:
            if (null == c || Void.TYPE.equals(c)) {
                return null;
            } else {
                throw new DBusException("Wrong return type (got void, expected a value)");
            }
        case 1:
            return rp[0];
        default:

            // check we are meant to return multiple values
            if (!Tuple.class.isAssignableFrom(c)) {
                throw new DBusException("Wrong return type (not expecting Tuple)");
            }

            Constructor<? extends Object> cons = c.getConstructors()[0];
            try {
                return cons.newInstance(rp);
            } catch (Exception e) {
                LOGGER.debug("", e);
                throw new DBusException(e.getMessage());
            }
        }
    }

    public static Object executeRemoteMethod(RemoteObject ro, Method m, AbstractConnection conn, int syncmethod, CallbackHandler<?> callback, Object... args) throws DBusException {
        Type[] ts = m.getGenericParameterTypes();
        String sig = null;
        if (ts.length > 0) {
            try {
                sig = Marshalling.getDBusType(ts);
                args = Marshalling.convertParameters(args, ts, conn);
            } catch (DBusException exDbe) {
                throw new DBusExecutionException("Failed to construct D-Bus type: " + exDbe.getMessage());
            }
        }
        MethodCall call;
        byte flags = 0;
        if (!ro.isAutostart()) {
            flags |= Message.Flags.NO_AUTO_START;
        }
        if (syncmethod == CALL_TYPE_ASYNC) {
            flags |= Message.Flags.ASYNC;
        }
        if (m.isAnnotationPresent(MethodNoReply.class)) {
            flags |= Message.Flags.NO_REPLY_EXPECTED;
        }
        try {
            String name;
            if (m.isAnnotationPresent(DBusMemberName.class)) {
                name = m.getAnnotation(DBusMemberName.class).value();
            } else {
                name = m.getName();
            }
            if (null == ro.getInterface()) {
                call = new MethodCall(ro.getBusName(), ro.getObjectPath(), null, name, flags, sig, args);
            } else {
                if (null != ro.getInterface().getAnnotation(DBusInterfaceName.class)) {
                    call = new MethodCall(ro.getBusName(), ro.getObjectPath(), ro.getInterface().getAnnotation(DBusInterfaceName.class).value(), name, flags, sig, args);
                } else {
                    call = new MethodCall(ro.getBusName(), ro.getObjectPath(), AbstractConnection.DOLLAR_PATTERN.matcher(ro.getInterface().getName()).replaceAll("."), name, flags, sig, args);
                }
            }
        } catch (DBusException dbe) {
            LOGGER.debug("Failed to construct outgoing method call.", dbe);
            throw new DBusExecutionException("Failed to construct outgoing method call: " + dbe.getMessage());
        }
        if (!conn.isConnected()) {
            throw new NotConnected("Not Connected");
        }

        switch (syncmethod) {
            case CALL_TYPE_ASYNC:
                conn.sendMessage(call);
                return new DBusAsyncReply<>(call, m, conn);
            case CALL_TYPE_CALLBACK:
                conn.queueCallback(call, m, callback);
                conn.sendMessage(call);
                return null;
            case CALL_TYPE_SYNC:
                conn.sendMessage(call);
                break;
        }

        // get reply
        if (m.isAnnotationPresent(MethodNoReply.class)) {
            return null;
        }

        Message reply = call.getReply();
        if (null == reply) {
            throw new NoReply("No reply within specified time");
        }

        if (reply instanceof Error) {
            ((Error) reply).throwException();
        }

        try {
            return convertRV(reply.getSig(), reply.getParameters(), m, conn);
        } catch (DBusException e) {
            LOGGER.debug("", e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

    // CHECKSTYLE:OFF
    AbstractConnection conn;
    RemoteObject       remote;
    // CHECKSTYLE:ON

    public RemoteInvocationHandler(AbstractConnection _conn, RemoteObject _remote) {
        this.remote = _remote;
        this.conn = _conn;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("isRemote")) {
            return true;
        } else if (method.getName().equals("getObjectPath")) {
            return remote.getObjectPath();
        } else if (method.getName().equals("clone")) {
            return null;
        } else if (method.getName().equals("equals")) {
            try {
                if (1 == args.length) {
                    return Boolean.valueOf(remote.equals(((RemoteInvocationHandler) Proxy.getInvocationHandler(args[0])).remote));
                }
            } catch (IllegalArgumentException exIa) {
                return Boolean.FALSE;
            }
        } else if (method.getName().equals("finalize")) {
            return null;
        } else if (method.getName().equals("getClass")) {
            return DBusInterface.class;
        } else if (method.getName().equals("hashCode")) {
            return remote.hashCode();
        } else if (method.getName().equals("notify")) {
            remote.notify();
            return null;
        } else if (method.getName().equals("notifyAll")) {
            remote.notifyAll();
            return null;
        } else if (method.getName().equals("wait")) {
            if (0 == args.length) {
                remote.wait();
            } else if (1 == args.length && args[0] instanceof Long) {
                remote.wait((Long) args[0]);
            } else if (2 == args.length && args[0] instanceof Long && args[1] instanceof Integer) {
                remote.wait((Long) args[0], (Integer) args[1]);
            }
            if (args.length <= 2) {
                return null;
            }
        } else if (method.getName().equals("toString")) {
            return remote.toString();
        }

        return executeRemoteMethod(remote, method, conn, CALL_TYPE_SYNC, null, args);
    }
}
