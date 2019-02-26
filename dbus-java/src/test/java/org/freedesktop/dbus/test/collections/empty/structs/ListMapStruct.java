/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class ListMapStruct extends Struct implements IEmptyCollectionStruct<List<Map<String,IntStruct>>> {

	@Position(0)
	private final List<Map<String,IntStruct>> list;

	@Position(1)
	private final String validationValue;

	public ListMapStruct(List<Map<String,IntStruct>> list, String validationValue) {
		this.list = list;
		this.validationValue = validationValue;
	}

	@Override
	public List<Map<String,IntStruct>> getValue() {
		return list;
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		return list.stream()
				.map(m -> toPrintableMap(m))
				.collect(Collectors.toList())
				.toString();
	}

	private Map<String, String> toPrintableMap(Map<String, IntStruct> m) {
		return m.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toSimpleString()));
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
}
