/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.types;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Wrapper class for Variant values.
 * A method on DBus can send or receive a Variant.
 * This will wrap another value whose type is determined at runtime.
 * The Variant may be parameterized to restrict the types it may accept.
 */
public class Variant<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final T      value;
    private final Type   type;
    private final String sig;

    /**
    * Create a Variant from a basic type object.
    * @param _value The wrapped value.
    * @throws IllegalArgumentException If you try and wrap Null or an object of a non-basic type.
    */
    public Variant(T _value) throws IllegalArgumentException {
        if (null == _value) {
            throw new IllegalArgumentException("Can't wrap Null in a Variant");
        }
        type = _value.getClass();
        try {
            String[] ss = Marshalling.getDBusType(_value.getClass(), true);
            if (ss.length != 1) {
                throw new IllegalArgumentException("Can't wrap a multi-valued type in a Variant: " + type);
            }
            this.sig = ss[0];
        } catch (DBusException dbe) {
            logger.debug("", dbe);
            throw new IllegalArgumentException(String.format("Can't wrap %s in an unqualified Variant (%s).", _value.getClass(), dbe.getMessage()));
        }
        this.value = _value;
    }

    /**
    * Create a Variant.
    * @param _value The wrapped value.
    * @param _type The explicit type of the value.
    * @throws IllegalArgumentException If you try and wrap Null or an object which cannot be sent over DBus.
    */
    public Variant(T _value, Type _type) throws IllegalArgumentException {
        if (null == _value) {
            throw new IllegalArgumentException("Can't wrap Null in a Variant");
        }
        this.type = _type;
        try {
            String[] ss = Marshalling.getDBusType(_type);
            if (ss.length != 1) {
                throw new IllegalArgumentException("Can't wrap a multi-valued type in a Variant: " + _type);
            }
            this.sig = ss[0];
        } catch (DBusException dbe) {
            logger.debug("", dbe);
            throw new IllegalArgumentException(String.format("Can't wrap %s in an unqualified Variant (%s).", _type, dbe.getMessage()));
        }
        this.value = _value;
    }

    /**
    * Create a Variant.
    * @param _value The wrapped value.
    * @param _sig The explicit type of the value, as a dbus type string.
    * @throws IllegalArgumentException If you try and wrap Null or an object which cannot be sent over DBus.
    */
    public Variant(T _value, String _sig) throws IllegalArgumentException {
        if (null == _value) {
            throw new IllegalArgumentException("Can't wrap Null in a Variant");
        }
        this.sig = _sig;
        try {
            List<Type> ts = new ArrayList<>();
            Marshalling.getJavaType(_sig, ts, 1);
            if (ts.size() != 1) {
                throw new IllegalArgumentException("Can't wrap multiple or no types in a Variant: " + _sig);
            }
            this.type = ts.get(0);
        } catch (DBusException dbe) {
            logger.debug("", dbe);
            throw new IllegalArgumentException(String.format("Can''t wrap %s in an unqualified Variant (%s).", _sig, dbe.getMessage()));
        }
        this.value = _value;
    }

    /** Return the wrapped value.
     * @return value
     */
    public T getValue() {
        return value;
    }

    /** Return the type of the wrapped value.
     *  @return type
     */
    public Type getType() {
        return type;
    }

    /** Return the dbus signature of the wrapped value.
     * @return signature
     */
    public String getSig() {
        return sig;
    }

    /** Format the Variant as a string. */
    @Override
    public String toString() {
        return "[" + value + "]";
    }

    /** Compare this Variant with another by comparing contents.
     * @param other other object
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        return null != other && other instanceof Variant && this.value.equals(((Variant<? extends Object>) other).value);
    }
}
