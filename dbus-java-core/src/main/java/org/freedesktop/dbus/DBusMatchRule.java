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

    public static Class<? extends DBusSignal> getCachedSignalType(String _type) {
        return SIGNALTYPEMAP.get(_type);
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

    public DBusMatchRule(DBusExecutionException _e) throws DBusException {
        this(_e.getClass());
        member = null;
        type = "error";
    }

    public DBusMatchRule(Message _m) {
        iface = _m.getInterface();
        member = _m.getName();
        if (_m instanceof DBusSignal) {
            type = "signal";
        } else if (_m instanceof Error) {
            type = "error";
            member = null;
        } else if (_m instanceof MethodCall) {
            type = "method_call";
        } else if (_m instanceof MethodReturn) {
            type = "method_reply";
        }
    }

    public DBusMatchRule(Class<? extends DBusInterface> _c, String _method) throws DBusException {
        this(_c);
        member = _method;
        type = "method_call";
    }

    public DBusMatchRule(Class<? extends Object> _c, String _source, String _object) throws DBusException {
        this(_c);
        this.source = _source;
        this.object = _object;
    }

    @SuppressWarnings("unchecked")
    public DBusMatchRule(Class<? extends Object> _c) throws DBusException {
        if (DBusInterface.class.isAssignableFrom(_c)) {
            if (null != _c.getAnnotation(DBusInterfaceName.class)) {
                iface = _c.getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(_c.getName()).replaceAll(".");
            }
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            member = null;
            type = null;
        } else if (DBusSignal.class.isAssignableFrom(_c)) {
            if (null == _c.getEnclosingClass()) {
                throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
            } else if (null != _c.getEnclosingClass().getAnnotation(DBusInterfaceName.class)) {
                iface = _c.getEnclosingClass().getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(_c.getEnclosingClass().getName()).replaceAll(".");
            }
            // Don't export things which are invalid D-Bus interfaces
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            if (_c.isAnnotationPresent(DBusMemberName.class)) {
                member = _c.getAnnotation(DBusMemberName.class).value();
            } else {
                member = _c.getSimpleName();
            }
            SIGNALTYPEMAP.put(iface + '$' + member, (Class<? extends DBusSignal>) _c);
            type = "signal";
        } else if (Error.class.isAssignableFrom(_c)) {
            if (null != _c.getAnnotation(DBusInterfaceName.class)) {
                iface = _c.getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(_c.getName()).replaceAll(".");
            }
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            member = null;
            type = "error";
        } else if (DBusExecutionException.class.isAssignableFrom(_c)) {
            if (null != _c.getClass().getAnnotation(DBusInterfaceName.class)) {
                iface = _c.getClass().getAnnotation(DBusInterfaceName.class).value();
            } else {
                iface = AbstractConnection.DOLLAR_PATTERN.matcher(_c.getClass().getName()).replaceAll(".");
            }
            if (!iface.matches(".*\\..*")) {
                throw new DBusException("DBusInterfaces must be defined in a package.");
            }
            member = null;
            type = "error";
        } else {
            throw new DBusException("Invalid type for match rule: " + _c);
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
