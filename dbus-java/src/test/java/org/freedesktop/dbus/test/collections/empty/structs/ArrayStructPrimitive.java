/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

public final class ArrayStructPrimitive extends Struct implements IEmptyCollectionStruct<int[]> {

	@Position(0)
	private final int[] list;

	@Position(1)
	private final String validationValue;

	public ArrayStructPrimitive(int[] list, String validationValue) {
		this.list = list.clone();
		this.validationValue = validationValue;
	}

	@Override
	public int[] getValue() {
		return list.clone();
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		return IntStream.of(list).mapToObj(i -> Integer.toString(i)).collect(Collectors.joining(","));
	}

	@Override
	public boolean isEmpty() {
		return list.length == 0;
	}

}
