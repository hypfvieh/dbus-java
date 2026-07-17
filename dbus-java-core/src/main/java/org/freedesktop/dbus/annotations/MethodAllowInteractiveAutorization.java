package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods allow interactive autorization
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.Method.AllowInteractiveAutorization")
public @interface MethodAllowInteractiveAutorization {

    /**
     * Annotation value, true by default
     *
     * @return true when a method allow interactive autorization, false otherwise
     */
    boolean value() default true;
}
