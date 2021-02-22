package org.freedesktop.dbus.annotations;

import org.freedesktop.dbus.types.Variant;

import java.lang.annotation.*;

/**
 * Appends information about properties in the interface. The annotated properties are added to the introspection data.
 * In case of complex type of the property please use {@link org.freedesktop.dbus.TypeRef}.
 * <p>
 * Usage:
 * </p>
 * <pre>{@code
 * @DBusInterfaceName("com.example.Bar")
 * @DBusProperty(name = "Name", type = String.class)
 * @DBusProperty(name = "ListOfVariables", type = List.class, access = Access.READ)
 * @DBusProperty(name = "MapOfStringList", type = ComplexTypeWithMapAndList.class, access = Access.READ)
 * public interface Bar extends DBusInterface {
 *
 *   // TypeRef allows to provide detailed information about type
 *   interface ComplexTypeWithMapAndList extends TypeRef<Map<String, List<String>>> {
 *   }
 * }
 * }</pre>
 *
 * @see org.freedesktop.dbus.interfaces.DBusInterface
 * @see org.freedesktop.dbus.TypeRef
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DBusProperties.class)
public @interface DBusProperty {

    /**
     * Property name
     *
     * @return name
     */
    String name();

    /**
     * type of the property, in case of complex types please create custom interface that extends {@link org.freedesktop.dbus.TypeRef}
     *
     * @return type
     */
    Class<?> type() default Variant.class;

    /**
     * Property access type
     *
     * @return access
     */
    Access access() default Access.READ_WRITE;

    enum Access {
        READ("read"),
        READ_WRITE("readwrite"),
        WRITE("write");

        private final String accessName;

        Access(String accessName) {
            this.accessName = accessName;
        }

        public String getAccessName() {
            return accessName;
        }
    }
}
