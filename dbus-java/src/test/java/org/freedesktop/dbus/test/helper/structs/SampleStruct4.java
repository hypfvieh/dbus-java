package org.freedesktop.dbus.test.helper.structs;

import java.util.List;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;


public final class SampleStruct4 extends Struct {
    @Position(0)
    private final List<IntStruct> innerListOfLists;

	public SampleStruct4(List<IntStruct> innerListOfLists) {
		this.innerListOfLists = innerListOfLists;
	}

	public List<IntStruct> getInnerListOfLists() {
		return innerListOfLists;
	}

}
