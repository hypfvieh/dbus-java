package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

import java.util.List;

public final class SampleStruct2 extends Struct {
    @Position(0)
    private final List<String>              valueList;
    @Position(1)
    private final Variant<? extends Object> variantValue;

    public SampleStruct2(List<String> _a, Variant<? extends Object> _b) {
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
