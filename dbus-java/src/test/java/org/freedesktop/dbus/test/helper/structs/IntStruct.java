/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV 

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
public final class IntStruct extends Struct {
    
	@Position(0)
    private final int value1;
    
    @Position(1)
    private final int value2;

	public IntStruct(int value1, int value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public int getValue1() {
		return value1;
	}

	public int getValue2() {
		return value2;
	}
	
	public String toSimpleString() {
		return String.format("(%s,%s)", value1, value2);
	}
}