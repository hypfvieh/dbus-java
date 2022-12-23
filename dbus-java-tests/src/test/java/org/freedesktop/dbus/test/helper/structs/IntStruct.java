package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

public final class IntStruct extends Struct {

    @Position(0)
    private final int value1;

    @Position(1)
    private final int value2;

    public IntStruct(int _value1, int _value2) {
        this.value1 = _value1;
        this.value2 = _value2;
    }

    public int getValue1() {
        return value1;
    }

    public int getValue2() {
        return value2;
    }

    public String toSimpleString() {
        return String.format("(%s,%s)", value1, value2);
    }
}
