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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;

public class DBusMatchRule {
    private static final Map<String, Class<? extends DBusSignal>> SIGNALTYPEMAP = new ConcurrentHashMap<>();

    /* signal, error, method_call, method_reply */
    private String                                              type;
    private String                                              iface;
    private String                                              member;
    private String                                              object;
    private String                                              source;

    public static Class<? extends DBusSignal> getCachedSignalType(String type) {
        return SIGNALTYPEMAP.get(type);
    }

    public DBusMatchRule(String _type, String _iface, String _member) {
        this.type = _type;
        this.iface = _iface;
        this.member = _member;
    }

    public DBusMatchRule(String _type, String _iface, String _member, String _object) {
        this.type = _type;
        this.iface = _iface;
        this.member = _member;
        this.object = _object;
    }

    public DBusMatchRule(DBusExecutionException e) throws DBusException {
        this(e.getClass());
        member = null;
        type = "error";
    }

    public DBusMatchRule(Message m) {
        iface = m.getInterface();
        member = m.getName();
        if (m instanceof DBusSignal) {
            type = "signal";
        } else if (m instanceof Error) {
            type = "error";
            member = null;
        } else if (m instanceof MethodCall) {
            type = "method_call";
        } else if (m instanceof MethodReturn) {
            type = "method_reply";
        }
    }

    public DBusMatchRule(Class<? extends DBusInterface> c, String method) throws DBusException {
        this(c);
        member = method;
        type = "method_call";
    }

    public DBusMatchRule(Class<? extends Object> _c, String _source, String _object) throws DBusException {
        this(_c);
        this.source = _source;
        this.object = _object;
    }

    @SuppressWarnings("unchecked")
    public DBusMatchRule(Class<? extends Object> c) throws DBusException {
        if (DBusInterface.class.isAssignableFrom(c)) {
            if (null != c.getAnnotation(DBusInterfaceName.class)) {
                iface = c.getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(c.getName()).replaceAll(".");
            }
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            member = null;
            type = null;
        } else if (DBusSignal.class.isAssignableFrom(c)) {
            if (null == c.getEnclosingClass()) {
                throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
            } else if (null != c.getEnclosingClass().getAnnotation(DBusInterfaceName.class)) {
                iface = c.getEnclosingClass().getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(c.getEnclosingClass().getName()).replaceAll(".");
            }
            // Don't export things which are invalid D-Bus interfaces
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            if (c.isAnnotationPresent(DBusMemberName.class)) {
                member = c.getAnnotation(DBusMemberName.class).value();
            } else {
                member = c.getSimpleName();
            }
            SIGNALTYPEMAP.put(iface + '$' + member, (Class<? extends DBusSignal>) c);
            type = "signal";
        } else if (Error.class.isAssignableFrom(c)) {
            if (null != c.getAnnotation(DBusInterfaceName.class)) {
                iface = c.getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(c.getName()).replaceAll(".");
            }
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            member = null;
            type = "error";
        } else if (DBusExecutionException.class.isAssignableFrom(c)) {
            if (null != c.getClass().getAnnotation(DBusInterfaceName.class)) {
                iface = c.getClass().getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(c.getClass().getName()).replaceAll(".");
            }
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            member = null;
            type = "error";
        } else {
            throw new DBusException("Invalid type for match rule: " + c);
        }
    }

    @Override
    public String toString() {
        String s = null;
        if (null != type) {
            s = null == s ? "type='" + type + "'" : s + ",type='" + type + "'";
        }
        if (null != member) {
            s = null == s ? "member='" + member + "'" : s + ",member='" + member + "'";
        }
        if (null != iface) {
            s = null == s ? "interface='" + iface + "'" : s + ",interface='" + iface + "'";
        }
        if (null != source) {
            s = null == s ? "sender='" + source + "'" : s + ",sender='" + source + "'";
        }
        if (null != object) {
            s = null == s ? "path='" + object + "'" : s + ",path='" + object + "'";
        }
        return s;
    }

    public String getType() {
        return type;
    }

    public String getInterface() {
        return iface;
    }

    public String getMember() {
        return member;
    }

    public String getSource() {
        return source;
    }

    public String getObject() {
        return object;
    }

}
