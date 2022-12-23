package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

import java.util.List;

public final class SampleStruct4 extends Struct {
    @Position(0)
    private final List<IntStruct> innerListOfLists;

    public SampleStruct4(List<IntStruct> _innerListOfLists) {
        this.innerListOfLists = _innerListOfLists;
    }

    public List<IntStruct> getInnerListOfLists() {
        return innerListOfLists;
    }

}
