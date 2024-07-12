package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

import java.util.List;

public final class SampleStruct3 extends Struct {
    @Position(0)
    private final SampleStruct2         innerStruct;
    @Position(1)
    private final List<List<Integer>> innerListOfLists;

    public SampleStruct3(SampleStruct2 _a, List<List<Integer>> _b) {
        this.innerStruct = _a;
        this.innerListOfLists = _b;
    }

    public SampleStruct2 getInnerStruct() {
        return innerStruct;
    }

    public List<List<Integer>> getInnerListOfLists() {
        return innerListOfLists;
    }
}
