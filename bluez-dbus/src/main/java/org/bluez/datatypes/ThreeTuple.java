package org.bluez.datatypes;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Generic 3 value tuple.
 * @author hypfvieh
 * @since v0.1.0 - 2018-03-07
 */
public class ThreeTuple<A, B, C> extends Tuple {

    @Position(0)
    private A firstValue;
    @Position(1)
    private B secondValue;
    @Position(2)
    private C thirdValue;

    public A getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(A _firstValue) {
        firstValue = _firstValue;
    }

    public B getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(B _secondValue) {
        secondValue = _secondValue;
    }

    public C getThirdValue() {
        return thirdValue;
    }

    public void setThirdValue(C _thirdValue) {
        thirdValue = _thirdValue;
    }

}
