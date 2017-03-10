/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import static org.freedesktop.dbus.Gettext.t;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Vector;

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
    private final T      o;
    private final Type   type;
    private final String sig;

    /**
    * Create a Variant from a basic type object.
    * @param _o The wrapped value.
    * @throws IllegalArgumentException If you try and wrap Null or an object of a non-basic type.
    */
    public Variant(T _o) throws IllegalArgumentException {
        if (null == _o) {
            throw new IllegalArgumentException(t("Can't wrap Null in a Variant"));
        }
        type = _o.getClass();
        try {
            String[] ss = Marshalling.getDBusType(_o.getClass(), true);
            if (ss.length != 1) {
                throw new IllegalArgumentException(t("Can't wrap a multi-valued type in a Variant: ") + type);
            }
            this.sig = ss[0];
        } catch (DBusException dbe) {
            if (AbstractConnection.EXCEPTION_DEBUG) {
                logger.error("", dbe);
            }
            throw new IllegalArgumentException(MessageFormat.format(t("Can't wrap {0} in an unqualified Variant ({1})."), new Object[] {
                    _o.getClass(), dbe.getMessage()
            }));
        }
        this.o = _o;
    }

    /**
    * Create a Variant.
    * @param _o The wrapped value.
    * @param _type The explicit type of the value.
    * @throws IllegalArgumentException If you try and wrap Null or an object which cannot be sent over DBus.
    */
    public Variant(T _o, Type _type) throws IllegalArgumentException {
        if (null == _o) {
            throw new IllegalArgumentException(t("Can't wrap Null in a Variant"));
        }
        this.type = _type;
        try {
            String[] ss = Marshalling.getDBusType(_type);
            if (ss.length != 1) {
                throw new IllegalArgumentException(t("Can't wrap a multi-valued type in a Variant: ") + _type);
            }
            this.sig = ss[0];
        } catch (DBusException dbe) {
            if (AbstractConnection.EXCEPTION_DEBUG) {
                logger.error("", dbe);
            }
            throw new IllegalArgumentException(MessageFormat.format(t("Can't wrap {0} in an unqualified Variant ({1})."), new Object[] {
                    _type, dbe.getMessage()
            }));
        }
        this.o = _o;
    }

    /**
    * Create a Variant.
    * @param _o The wrapped value.
    * @param _sig The explicit type of the value, as a dbus type string.
    * @throws IllegalArgumentException If you try and wrap Null or an object which cannot be sent over DBus.
    */
    public Variant(T _o, String _sig) throws IllegalArgumentException {
        if (null == _o) {
            throw new IllegalArgumentException(t("Can't wrap Null in a Variant"));
        }
        this.sig = _sig;
        try {
            Vector<Type> ts = new Vector<Type>();
            Marshalling.getJavaType(_sig, ts, 1);
            if (ts.size() != 1) {
                throw new IllegalArgumentException(t("Can't wrap multiple or no types in a Variant: ") + _sig);
            }
            this.type = ts.get(0);
        } catch (DBusException dbe) {
            if (AbstractConnection.EXCEPTION_DEBUG) {
                logger.error("", dbe);
            }
            throw new IllegalArgumentException(MessageFormat.format(t("Can't wrap {0} in an unqualified Variant ({1})."), new Object[] {
                    _sig, dbe.getMessage()
            }));
        }
        this.o = _o;
    }

    /** Return the wrapped value.
     * @return value
     */
    public T getValue() {
        return o;
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
        return "[" + o + "]";
    }

    /** Compare this Variant with another by comparing contents.
     * @param other other object
     * @return boolean
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }
        if (!(other instanceof Variant)) {
            return false;
        }
        return this.o.equals(((Variant<? extends Object>) other).o);
    }
}
