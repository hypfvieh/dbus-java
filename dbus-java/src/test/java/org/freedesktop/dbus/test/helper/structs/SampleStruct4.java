/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV 

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

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
