/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.Map;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;


public final class MapStructPrimitive extends Struct implements IEmptyCollectionStruct<Map<String, Integer>> {

	@Position(0)
	private final Map<String, Integer> map;

	@Position(1)
	private final String validationValue;

	public MapStructPrimitive(Map<String, Integer> map, String validationValue) {
		this.map = map;
		this.validationValue = validationValue;
	}

	@Override
	public Map<String, Integer> getValue() {
		return map;
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		String values = map.entrySet().stream()
				.map(e -> String.format("%s:%s", e.getKey(), e.getValue()))
				.collect(Collectors.joining(","));
		return String.format("{%s}", values);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

}
