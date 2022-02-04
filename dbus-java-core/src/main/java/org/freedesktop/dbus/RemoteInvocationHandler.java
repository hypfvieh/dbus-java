package org.freedesktop.dbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

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
import org.freedesktop.dbus.utils.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteInvocationHandler implements InvocationHandler {
    public static final int CALL_TYPE_SYNC     = 0;
    public static final int CALL_TYPE_ASYNC    = 1;
    public static final int CALL_TYPE_CALLBACK = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteInvocationHandler.class);
    // CHECKSTYLE:OFF
    AbstractConnection conn;
    RemoteObject       remote;
    // CHECKSTYLE:ON



    public static Object convertRV(String _sig, Object[] _rp, Method _m, AbstractConnection _conn) throws DBusException {
        Class<? extends Object> c = _m.getReturnType();

        if (null == _rp) {
            if (null == c || Void.TYPE.equals(c)) {
                return null;
            } else {
                throw new DBusException("Wrong return type (got void, expected a value)");
            }
        } else {
            try {
                LOGGER.trace("Converting return parameters from {} to type {}",LoggingHelper.arraysDeepString(LOGGER.isTraceEnabled(), _rp), _m.getGenericReturnType());
                _rp = Marshalling.deSerializeParameters(_rp, new Type[] {
                        _m.getGenericReturnType()
                }, _conn);
            } catch (Exception e) {
                LOGGER.debug("Wrong return type.", e);
                throw new DBusException(String.format("Wrong return type (failed to de-serialize correct types: %s )", e.getMessage()));
            }
        }

        switch (_rp.length) {
        case 0:
            if (null == c || Void.TYPE.equals(c)) {
                return null;
            } else {
                throw new DBusException("Wrong return type (got void, expected a value)");
            }
        case 1:
            return _rp[0];
        default:

            // check we are meant to return multiple values
            if (!Tuple.class.isAssignableFrom(c)) {
                throw new DBusException("Wrong return type (not expecting Tuple)");
            }

            Constructor<? extends Object> cons = c.getConstructors()[0];
            try {
                return cons.newInstance(_rp);
            } catch (Exception e) {
                LOGGER.debug("", e);
                throw new DBusException(e.getMessage());
            }
        }
    }

    public static Object executeRemoteMethod(RemoteObject _ro, Method _m, AbstractConnection _conn, int _syncmethod, CallbackHandler<?> _callback, Object... _args) throws DBusException {
        Type[] ts = _m.getGenericParameterTypes();
        String sig = null;
        if (ts.length > 0) {
            try {
                sig = Marshalling.getDBusType(ts);
                _args = Marshalling.convertParameters(_args, ts, _conn);
            } catch (DBusException exDbe) {
                throw new DBusExecutionException("Failed to construct D-Bus type: " + exDbe.getMessage());
            }
        }
        MethodCall call;
        byte flags = 0;
        if (!_ro.isAutostart()) {
            flags |= Message.Flags.NO_AUTO_START;
        }
        if (_syncmethod == CALL_TYPE_ASYNC) {
            flags |= Message.Flags.ASYNC;
        }
        if (_m.isAnnotationPresent(MethodNoReply.class)) {
            flags |= Message.Flags.NO_REPLY_EXPECTED;
        }
        try {
            String name;
            if (_m.isAnnotationPresent(DBusMemberName.class)) {
                name = _m.getAnnotation(DBusMemberName.class).value();
            } else {
                name = _m.getName();
            }
            if (null == _ro.getInterface()) {
                call = new MethodCall(_ro.getBusName(), _ro.getObjectPath(), null, name, flags, sig, _args);
            } else {
                if (null != _ro.getInterface().getAnnotation(DBusInterfaceName.class)) {
                    call = new MethodCall(_ro.getBusName(), _ro.getObjectPath(), _ro.getInterface().getAnnotation(DBusInterfaceName.class).value(), name, flags, sig, _args);
                } else {
                    call = new MethodCall(_ro.getBusName(), _ro.getObjectPath(), AbstractConnection.DOLLAR_PATTERN.matcher(_ro.getInterface().getName()).replaceAll("."), name, flags, sig, _args);
                }
            }
        } catch (DBusException dbe) {
            LOGGER.debug("Failed to construct outgoing method call.", dbe);
            throw new DBusExecutionException("Failed to construct outgoing method call: " + dbe.getMessage());
        }
        if (!_conn.isConnected()) {
            throw new NotConnected("Not Connected");
        }

        switch (_syncmethod) {
            case CALL_TYPE_ASYNC:
                _conn.sendMessage(call);
                return new DBusAsyncReply<>(call, _m, _conn);
            case CALL_TYPE_CALLBACK:
                _conn.queueCallback(call, _m, _callback);
                _conn.sendMessage(call);
                return null;
            case CALL_TYPE_SYNC:
                _conn.sendMessage(call);
                break;
        }

        // get reply
        if (_m.isAnnotationPresent(MethodNoReply.class)) {
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
            return convertRV(reply.getSig(), reply.getParameters(), _m, _conn);
        } catch (DBusException e) {
            LOGGER.debug("", e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

  
    public RemoteInvocationHandler(AbstractConnection _conn, RemoteObject _remote) {
        this.remote = _remote;
        this.conn = _conn;
    }

    @Override
    public Object invoke(Object _proxy, Method _method, Object[] _args) throws Throwable {
        if (_method.getName().equals("isRemote")) {
            return true;
        } else if (_method.getName().equals("getObjectPath")) {
            return remote.getObjectPath();
        } else if (_method.getName().equals("clone")) {
            return null;
        } else if (_method.getName().equals("equals")) {
            try {
                if (1 == _args.length) {
                    return Boolean.valueOf(_args[0] != null && remote.equals(((RemoteInvocationHandler) Proxy.getInvocationHandler(_args[0])).remote));
                }
            } catch (IllegalArgumentException exIa) {
                return Boolean.FALSE;
            }
        } else if (_method.getName().equals("finalize")) {
            return null;
        } else if (_method.getName().equals("getClass")) {
            return DBusInterface.class;
        } else if (_method.getName().equals("hashCode")) {
            return remote.hashCode();
        } else if (_method.getName().equals("notify")) {
            remote.notify();
            return null;
        } else if (_method.getName().equals("notifyAll")) {
            remote.notifyAll();
            return null;
        } else if (_method.getName().equals("wait")) {
            if (0 == _args.length) {
                remote.wait();
            } else if (1 == _args.length && _args[0] instanceof Long) {
                remote.wait((Long) _args[0]);
            } else if (2 == _args.length && _args[0] instanceof Long && _args[1] instanceof Integer) {
                remote.wait((Long) _args[0], (Integer) _args[1]);
            }
            if (_args.length <= 2) {
                return null;
            }
        } else if (_method.getName().equals("toString")) {
            return remote.toString();
        }

        return executeRemoteMethod(remote, _method, conn, CALL_TYPE_SYNC, null, _args);
    }
}
