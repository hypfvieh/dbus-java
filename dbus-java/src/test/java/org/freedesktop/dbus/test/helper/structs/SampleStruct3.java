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

import java.util.List;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;


public final class SampleStruct3 extends Struct {
    @Position(0)
    private final SampleStruct2         innerStruct;
    @Position(1)
    private final List<List<Integer>> innerListOfLists;

    public SampleStruct3(SampleStruct2 _a, List<List<Integer>> _b) throws DBusException {
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
