package org.freedesktop.dbus.annotations;

import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.Properties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a <strong>setter</strong> or <strong>getter</strong> method to a DBus property, in
 * a similar manner to the familiar JavaBeans pattern.
 * <p>
 * Using this annotation means you do not need to implement the {@link Properties}
 * interface and provide your own handling of {@link Properties#Get(String, String)},
 * {@link Properties#GetAll(String)} and {@link Properties#Set(String, String, Object)}.
 * <p>
 * Each DBus property should map to either one or two methods. If it has
 * {@link DBusBoundProperty#access()} of {@link Access#READ} then a single <strong>getter</strong>
 * method should be created with no parameters. The type of property will be determined by
 * the return type of the method, and the name of the property will be derived from the method name,
 * with either the <code>get</code> or the <code>is</code> stripped off.
 * <pre>
 * {@literal @}DBusBoundProperty
 * public String getMyStringProperty();
 *
 * {@literal @}DBusBoundProperty
 * public boolean isMyBooleanProperty();
 * </pre>
 * If it has {@link DBusBoundProperty#access()} of {@link Access#WRITE} then a single <strong>setter</strong>
 * method should be created with a single parameter and no return type. The type of the property
 * will be determined by that parameter, and the name of the property will be derived from the
 * method name, with either the <code>get</code> or the <code>is</code> stripped off.
 * <pre>
 * {@literal @}DBusBoundProperty
 * public void setMyStringProperty(String _property);
 * </pre>
 * If it has {@link DBusBoundProperty#access()} of {@link Access#READ_WRITE}, the both of
 * the above methods should be provided.
 * <p>
 * Any of the <code>name</code>, <code>type</code> and <code>access</code> attributes that would
 * normally be automatically determined, may be overridden using the corresponding annotation attributes.
 * <p>
 * It is allowed if you wish to mix use of {@link DBusProperty} and {@link DBusBoundProperty} as
 * long as individual properties are not repeated.
 *
 * @see org.freedesktop.dbus.interfaces.DBusInterface
 * @see org.freedesktop.dbus.annotations.DBusProperty
 * @see org.freedesktop.dbus.annotations.DBusProperties
 * @see org.freedesktop.dbus.TypeRef
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DBusBoundProperty {

    /**
     * Property name. If not supplied, the property name will be inferred from the method. See
     * class documentation for semantics.
     *
     * @return name
     */
    String name() default "";

    /**
     * Type of the property, in case of complex types please create custom interface that extends {@link org.freedesktop.dbus.TypeRef}.
     * If not supplied, then the type will be inferred from either the return value of a getter, or
     * the first parameter of a setter.
     *
     * @return type
     */
    Class<?> type() default Void.class;

    /**
     * Property access type. When {@link Access#READ_WRITE}, the access will be inferred from
     * the method name, whether it is a setter or a getter.
     *
     * @return access
     */
    Access access() default Access.READ_WRITE;

}
