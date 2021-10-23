package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class MapArrayStruct extends Struct implements IEmptyCollectionStruct<Map<String,IntStruct[]>> {

	@Position(0)
	private final Map<String,IntStruct[]> map;

	@Position(1)
	private final String validationValue;

	public MapArrayStruct(Map<String,IntStruct[]> map, String validationValue) {
		this.map = map;
		this.validationValue = validationValue;
	}

	@Override
	public Map<String,IntStruct[]> getValue() {
		return map;
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		return map.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> toPrintableArray(e.getValue())))
				.toString();
	}

	private String toPrintableArray(IntStruct[] array) {
		String values = Stream.of(array).map(e -> e.toSimpleString())
				.collect(Collectors.joining(","));
		return String.format("[%s]", values);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}
}
