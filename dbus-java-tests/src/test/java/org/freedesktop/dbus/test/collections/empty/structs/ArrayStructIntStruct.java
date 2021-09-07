package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class ArrayStructIntStruct extends Struct implements IEmptyCollectionStruct<IntStruct[]> {

	@Position(0)
	private final IntStruct[] list;

	@Position(1)
	private final String validationValue;

	public ArrayStructIntStruct(IntStruct[] list, String validationValue) {
		this.list = list.clone();
		this.validationValue = validationValue;
	}

	@Override
	public IntStruct[] getValue() {
		return list.clone();
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		return Stream.of(list)
				.map(i -> i.toSimpleString())
				.collect(Collectors.joining(","));
	}

	@Override
	public boolean isEmpty() {
		return list.length == 0;
	}

}
