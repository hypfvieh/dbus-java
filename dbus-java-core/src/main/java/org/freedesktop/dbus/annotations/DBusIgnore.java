package org.freedesktop.dbus.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an exported method as ignored. It will not be included in introspection data, and it will not be
 * remotely callable. This is only useful for a local DBus object, it has no meaning to remote objects.
 * <p>
 * Usage:
 * </p>
 * <pre>
 * {@literal @}DBusInterfaceName("com.example.Bar")
 * public interface Bar extends DBusInterface {
 *
 *     @DBusIgnore
 *     public void doSomethingInternal() {
 *     }
 * }
 * </pre>
 *
 * @see org.freedesktop.dbus.interfaces.DBusInterface
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DBusIgnore {
}
