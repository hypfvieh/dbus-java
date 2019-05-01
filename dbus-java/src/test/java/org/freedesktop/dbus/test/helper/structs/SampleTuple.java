/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

//CHECKSTYLE:OFF
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
//CHECKSTYLE:ON