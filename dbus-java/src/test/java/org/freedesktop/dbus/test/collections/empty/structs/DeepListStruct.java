package org.freedesktop.dbus.test.collections.empty.structs;

import java.util.List;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class DeepListStruct extends Struct implements IEmptyCollectionStruct<List<List<List<IntStruct>>>> {

	@Position(0)
	private final List<List<List<IntStruct>>> list;

	@Position(1)
	private final String validationValue;

	public DeepListStruct(List<List<List<IntStruct>>> list, String validationValue) {
		this.list = list;
		this.validationValue = validationValue;
	}

	@Override
	public List<List<List<IntStruct>>> getValue() {
		return list;
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		String string = "[";
			for (List<List<IntStruct>> l1 : list) {
				string += "[";
				for (List<IntStruct> l2 : l1) {
					string += "[";
					for (IntStruct e : l2) {
						string += e.toSimpleString();
					}
					string += "]";
				}
				string += "]";
			}
			string += "]";

		return string;

	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
}
