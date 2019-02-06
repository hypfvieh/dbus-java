/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test.helper.structs;

import java.util.List;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;
public final class SampleStruct2 extends Struct {
    @Position(0)
    private final List<String>              valueList;
    @Position(1)
    private final Variant<? extends Object> variantValue;

    public SampleStruct2(List<String> _a, Variant<? extends Object> _b) throws DBusException {
        this.valueList = _a;
        this.variantValue = _b;
    }

    public List<String> getValueList() {
        return valueList;
    }

    public Variant<? extends Object> getVariantValue() {
        return variantValue;
    }
    
    
}