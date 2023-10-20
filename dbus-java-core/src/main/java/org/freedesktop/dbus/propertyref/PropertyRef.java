package org.freedesktop.dbus.propertyref;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Contains the same information as a {@link DBusBoundProperty}, but as a POJO. Use
 * internally when dealing with properties that are derived from methods annotated
 * with said annotation.
 *
 * @author Brett Smith
 * @since 5.0.0 - 2023-10-20
 */
public final  class PropertyRef {

    private final String name;
    private final Class<?> type;
    private final DBusProperty.Access access;

    public PropertyRef(String _name, Class<?> _type, Access _access) {
        super();
        this.name = _name;
        this.type = _type;
        this.access = _access;
    }

    public PropertyRef(DBusProperty _property) {
        this(_property.name(), _property.type(), _property.access());
    }

    @Override
    public int hashCode() {
        return Objects.hash(access, name);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        }
        if (_obj == null) {
            return false;
        }
        if (getClass() != _obj.getClass()) {
            return false;
        }
        PropertyRef other = (PropertyRef) _obj;
        return access == other.access && Objects.equals(name, other.name);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public DBusProperty.Access getAccess() {
        return access;
    }

    public static Access accessForMethod(Method _method) {
        DBusBoundProperty annotation = _method.getAnnotation(DBusBoundProperty.class);
        Access access = _method.getName().toLowerCase().startsWith("set") ? Access.WRITE : Access.READ;
        if (annotation.access().equals(Access.READ) || annotation.access().equals(Access.WRITE)) {
            access = annotation.access();
        }
        return access;
    }

    public static Class<?> typeForMethod(Method _method) {
        DBusBoundProperty annotation = _method.getAnnotation(DBusBoundProperty.class);
        Class<?> type = annotation.type();
        if (type == null || type.equals(Void.class)) {
            if (accessForMethod(_method) == Access.READ) {
                return _method.getReturnType();
            } else {
                return _method.getParameterTypes()[0];
            }
        }
        return type;
    }

    public static void checkMethod(Method _method) {
        Access access = accessForMethod(_method);
        if (access == Access.READ && (_method.getParameterCount() > 0 || _method.getReturnType().equals(void.class))) {
            throw new IllegalArgumentException("READ properties must have zero parameters, and not return void.");
        }
        if (access == Access.WRITE && (_method.getParameterCount() != 1 || !_method.getReturnType().equals(void.class))) {
            throw new IllegalArgumentException("WRITE properties must have exaclty 1 parameter, and return void.");
        }
    }
}
