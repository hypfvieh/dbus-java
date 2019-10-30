package org.bluez.datatypes;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Generic 2 value tuple.
 * @author hypfvieh
 * @since v0.1.0 - 2018-03-07
 */
public class TwoTuple<A,B> extends Tuple {

    @Position(0)
    private A firstValue;
    @Position(1)
    private B secondValue;


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


}
