package org.freedesktop.dbus.propertyref;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Contains {@link DBusBoundProperty} code used for remote method invokation.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-20
 */
public final class PropRefRemoteHandler {
    private static final Method PROP_GET_METHOD = getPropertiesMethod("Get", String.class, String.class);
    private static final Method PROP_SET_METHOD = getPropertiesMethod("Set", String.class, String.class, Object.class);

    private PropRefRemoteHandler() {

    }

    /**
     * Handles the {@link DBusBoundProperty} annotation.<br>
     * <br>
     * The special handling is needed because when this annotation is used the<br>
     * object uses the DBus {@link Properties} interface without explicitly implementing it.<br>
     * <br>
     * In that case we have to fake the presents of this interface and also take care about<br>
     * the types which have to be wrapped in {@link Variant}<br>
     * (due to DBus Properties specify that every property value is a {@link Variant}).<br>
     *
     * @param _conn connection
     * @param _remote remote object
     * @param _method annotated method which was called
     * @param _args arguments used if this was a call to a setter
     * @return remote invocation result
     *
     * @throws DBusException when DBus call fails
     * @throws NoSuchMethodException when method was not found
     */
    public static Object handleDBusBoundProperty(AbstractConnection _conn, RemoteObject _remote, Method _method, Object[] _args) throws DBusException, NoSuchMethodException {
        String name = DBusNamingUtil.getPropertyName(_method);
        Access access = PropertyRef.accessForMethod(_method);

        Class<?> typeClass = _method.getAnnotation(DBusBoundProperty.class).type();
        Type[] type = null;
        // take care about wrapped types defined by TypeRef interface
        if (TypeRef.class.isAssignableFrom(typeClass)) {
            type = Optional.ofNullable(Util.unwrapTypeRef(typeClass))
                .map(t -> new Type[] {t})
                .orElse(null);
        }

        String[] variantType = type != null ? new String[] {Marshalling.getDBusType(type)} : null;

        if (access == Access.READ) {
            return RemoteInvocationHandler.executeRemoteMethod(_remote, PROP_GET_METHOD,
                   new Type[] {_method.getGenericReturnType()}, _conn, RemoteInvocationHandler.CALL_TYPE_SYNC, null, DBusNamingUtil.getInterfaceName(_method.getDeclaringClass()), name);
        } else {
            return RemoteInvocationHandler.executeRemoteMethod(_remote, PROP_SET_METHOD, variantType,
                   new Type[] {_method.getGenericReturnType()}, _conn, RemoteInvocationHandler.CALL_TYPE_SYNC, null, DBusNamingUtil.getInterfaceName(_method.getDeclaringClass()), name, _args[0]);
        }
    }

    private static Method getPropertiesMethod(String _method, Class<?>... _signature) {
        try {
            return Properties.class.getMethod(_method, _signature);
        } catch (NoSuchMethodException | SecurityException _ex) {
            throw new RuntimeException("Unable to get methods of DBus Properties interface", _ex);
        }
    }
}
