/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

public final class ArrayFrob {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new ConcurrentHashMap<>();
    static {
        PRIMITIVE_TO_WRAPPER.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(Byte.TYPE, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(Short.TYPE, Short.class);
        PRIMITIVE_TO_WRAPPER.put(Character.TYPE, Character.class);
        PRIMITIVE_TO_WRAPPER.put(Integer.TYPE, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(Long.TYPE, Long.class);
        PRIMITIVE_TO_WRAPPER.put(Float.TYPE, Float.class);
        PRIMITIVE_TO_WRAPPER.put(Double.TYPE, Double.class);
        WRAPPER_TO_PRIMITIVE.put(Boolean.class, Boolean.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Byte.class, Byte.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Short.class, Short.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Character.class, Character.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Integer.class, Integer.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Long.class, Long.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Float.class, Float.TYPE);
        WRAPPER_TO_PRIMITIVE.put(Double.class, Double.TYPE);

    }

    public static Map<Class<?>, Class<?>> getPrimitiveToWrapperTypes() {
        return Collections.unmodifiableMap(PRIMITIVE_TO_WRAPPER);
    }

    public static Map<Class<?>, Class<?>> getWrapperToPrimitiveTypes() {
        return Collections.unmodifiableMap(WRAPPER_TO_PRIMITIVE);
    }

    private ArrayFrob() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] wrap(Object o) throws IllegalArgumentException {
        Class<? extends Object> ac = o.getClass();
        if (!ac.isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        Class<? extends Object> cc = ac.getComponentType();
        Class<? extends Object> ncc = PRIMITIVE_TO_WRAPPER.get(cc);
        if (null == ncc) {
            throw new IllegalArgumentException("Not a primitive type");
        }
        T[] ns = (T[]) Array.newInstance(ncc, Array.getLength(o));
        for (int i = 0; i < ns.length; i++) {
            ns[i] = (T) Array.get(o, i);
        }
        return ns;
    }

    @SuppressWarnings("unchecked")
    public static <T> Object unwrap(T[] ns) throws IllegalArgumentException {
        Class<? extends T[]> ac = (Class<? extends T[]>) ns.getClass();
        Class<T> cc = (Class<T>) ac.getComponentType();
        Class<? extends Object> ncc = WRAPPER_TO_PRIMITIVE.get(cc);
        if (null == ncc) {
            throw new IllegalArgumentException("Not a wrapper type");
        }
        Object o = Array.newInstance(ncc, ns.length);
        for (int i = 0; i < ns.length; i++) {
            Array.set(o, i, ns[i]);
        }
        return o;
    }

    public static <T> List<T> listify(T[] ns) throws IllegalArgumentException {
        return Arrays.asList(ns);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> listify(Object o) throws IllegalArgumentException {
        if (o instanceof Object[]) {
            return listify((T[]) o);
        }
        if (!o.getClass().isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        List<T> l = new ArrayList<>(Array.getLength(o));
        for (int i = 0; i < Array.getLength(o); i++) {
            l.add((T) Array.get(o, i));
        }
        return l;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] delist(List<T> l, Class<T> c) throws IllegalArgumentException {
        return l.toArray((T[]) Array.newInstance(c, 0));
    }

    public static <T> Object delistprimitive(List<T> l, Class<T> c) throws IllegalArgumentException {
        Object o = Array.newInstance(c, l.size());
        for (int i = 0; i < l.size(); i++) {
            Array.set(o, i, l.get(i));
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    public static Object convert(Object o, Class<? extends Object> c) throws IllegalArgumentException {
        /* Possible Conversions:
         *
         ** List<Integer> -> List<Integer>
         ** List<Integer> -> int[]
         ** List<Integer> -> Integer[]
         ** int[] -> int[]
         ** int[] -> List<Integer>
         ** int[] -> Integer[]
         ** Integer[] -> Integer[]
         ** Integer[] -> int[]
         ** Integer[] -> List<Integer>
         */
        try {
            // List<Integer> -> List<Integer>
            if (List.class.equals(c) && o instanceof List) {
                return o;
            }

            // int[] -> List<Integer>
            // Integer[] -> List<Integer>
            if (List.class.equals(c) && o.getClass().isArray()) {
                return listify(o);
            }

            // int[] -> int[]
            // Integer[] -> Integer[]
            if (o.getClass().isArray() && c.isArray() && o.getClass().getComponentType().equals(c.getComponentType())) {
                return o;
            }

            // int[] -> Integer[]
            if (o.getClass().isArray() && c.isArray() && o.getClass().getComponentType().isPrimitive()) {
                return wrap(o);
            }

            // Integer[] -> int[]
            if (o.getClass().isArray() && c.isArray() && c.getComponentType().isPrimitive()) {
                return unwrap((Object[]) o);
            }

            // List<Integer> -> int[]
            if (o instanceof List && c.isArray() && c.getComponentType().isPrimitive()) {
                return delistprimitive((List<Object>) o, (Class<Object>) c.getComponentType());
            }

            // List<Integer> -> Integer[]
            if (o instanceof List && c.isArray()) {
                return delist((List<Object>) o, (Class<Object>) c.getComponentType());
            }

            if (o.getClass().isArray() && c.isArray()) {
                return type((Object[]) o, (Class<Object>) c.getComponentType());
            }

        } catch (Exception e) {
            LoggerFactory.getLogger(ArrayFrob.class).debug("Cannot convert object.", e);
            throw new IllegalArgumentException(e);
        }

        throw new IllegalArgumentException(String.format("Not An Expected Convertion type from %s to %s", o.getClass(), c));
    }

    public static Object[] type(Object[] old, Class<Object> c) {
        Object[] ns = (Object[]) Array.newInstance(c, old.length);
        for (int i = 0; i < ns.length; i++) {
            ns[i] = old[i];
        }
        return ns;
    }
}
