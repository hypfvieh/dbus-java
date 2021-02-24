package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.List;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class ListStructStruct extends Struct implements IEmptyCollectionStruct<List<IntStruct>> {

	@Position(0)
	private final List<IntStruct> list;

	@Position(1)
	private final String validationValue;

	public ListStructStruct(List<IntStruct> list, String validationValue) {
		this.list = list;
		this.validationValue = validationValue;
	}

	@Override
	public List<IntStruct> getValue() {
		return list;
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		return list.stream()
				.map(i -> i.toSimpleString())
				.collect(Collectors.joining(","));
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
}
