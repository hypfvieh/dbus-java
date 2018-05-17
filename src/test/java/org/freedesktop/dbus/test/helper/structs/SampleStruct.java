/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

public final class SampleStruct extends Struct {
    @Position(0)
    private final String                    stringValue;
    @Position(1)
    private final UInt32                    int32Value;
    @Position(2)
    private final Variant<?>                variantValue;

    public SampleStruct(String _a, UInt32 _b, Variant<?> _c) {
        this.stringValue = _a;
        this.int32Value = _b;
        this.variantValue = _c;
    }

    public String getStringValue() {
        return stringValue;
    }

    public UInt32 getInt32Value() {
        return int32Value;
    }

    public Variant<?> getVariantValue() {
        return variantValue;
    }

    
}
