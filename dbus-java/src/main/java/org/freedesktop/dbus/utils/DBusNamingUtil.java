package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * DBus name Util class for internal and external use.
 */
public final class DBusNamingUtil {
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("[$]");

    private DBusNamingUtil() {
    }

    /**
     * Get DBus interface name for specified interface class
     *
     * @param clazz input DBus interface class
     * @return interface name
     * @see DBusInterfaceName
     */
    public static String getInterfaceName(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");

        if (clazz.isAnnotationPresent(DBusInterfaceName.class)) {
            return clazz.getAnnotation(DBusInterfaceName.class).value();
        }
        return DOLLAR_PATTERN.matcher(clazz.getName()).replaceAll(".");
    }

    /**
     * Get DBus method name for specified method object.
     *
     * @param method input method
     * @return method name
     * @see DBusMemberName
     */
    public static String getMethodName(Method method) {
        Objects.requireNonNull(method, "method must not be null");

        if (method.isAnnotationPresent(DBusMemberName.class)) {
            return method.getAnnotation(DBusMemberName.class).value();
        }
        return method.getName();
    }

    /**
     * Get DBus signal name for specified signal class.
     *
     * @param clazz input DBus signal class
     * @return signal name
     * @see DBusMemberName
     */
    public static String getSignalName(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");

        if (clazz.isAnnotationPresent(DBusMemberName.class)) {
            return clazz.getAnnotation(DBusMemberName.class).value();
        }
        return clazz.getSimpleName();
    }

    /**
     * Get DBus name for specified annotation class
     *
     * @param clazz input DBus annotation
     * @return interface name
     * @see DBusInterfaceName
     */
    public static String getAnnotationName(Class<? extends Annotation> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");

        if (clazz.isAnnotationPresent(DBusInterfaceName.class)) {
            return clazz.getAnnotation(DBusInterfaceName.class).value();
        }
        return DOLLAR_PATTERN.matcher(clazz.getName()).replaceAll(".");
    }
}
