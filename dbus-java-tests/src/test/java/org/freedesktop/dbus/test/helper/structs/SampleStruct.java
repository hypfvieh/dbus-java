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
