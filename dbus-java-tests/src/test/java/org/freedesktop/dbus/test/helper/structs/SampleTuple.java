package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

public final class SampleTuple<A, B, C> extends Tuple {
    @Position(0)
    private final A firstValue;
    @Position(1)
    private final B secondValue;
    @Position(2)
    private final C thirdValue;

    public SampleTuple(A _a, B _b, C _c) {
        this.firstValue = _a;
        this.secondValue = _b;
        this.thirdValue = _c;
    }

    public A getFirstValue() {
        return firstValue;
    }

    public B getSecondValue() {
        return secondValue;
    }

    public C getThirdValue() {
        return thirdValue;
    }

}
