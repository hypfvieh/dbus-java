/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.freedesktop.dbus.Struct;

/**
 * The type of a struct.
 * Should be used whenever you need a Type variable for a struct.
 */
public class DBusStructType implements ParameterizedType {
    private Type[] contents;

    /**
    * Create a struct type.
    * @param _contents The types contained in this struct.
    */
    public DBusStructType(Type... _contents) {
        this.contents = _contents;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return contents;
    }

    @Override
    public Type getRawType() {
        return Struct.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
