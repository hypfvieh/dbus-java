package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/** Just a typed container class */
@SuppressWarnings({"checkstyle:visibilitymodifier"})
public final class Pair<A, B> extends Tuple {
    @Position(0)
    public final A a;
    @Position(1)
    public final B b;

    public Pair(A _a, B _b) {
        this.a = _a;
        this.b = _b;
    }
}
