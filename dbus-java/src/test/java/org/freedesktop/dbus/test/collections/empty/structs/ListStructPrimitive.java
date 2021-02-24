package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.List;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

public final class ListStructPrimitive extends Struct implements IEmptyCollectionStruct<List<Integer>> {

	@Position(0)
	private final List<Integer> list;

	@Position(1)
	private final String validationValue;

	public ListStructPrimitive(List<Integer> list, String validationValue) {
		this.list = list;
		this.validationValue = validationValue;
	}

	@Override
	public List<Integer> getValue() {
		return list;
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		return list.stream().map(i -> i.toString()).collect(Collectors.joining(","));
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

}
