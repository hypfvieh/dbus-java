package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class DeepArrayStruct extends Struct implements IEmptyCollectionStruct<IntStruct[][][]> {

	@Position(0)
	private final IntStruct[][][] list;

	@Position(1)
	private final String validationValue;

	public DeepArrayStruct(IntStruct[][][] list, String validationValue) {
		this.list = list.clone();
		this.validationValue = validationValue;
	}

	@Override
	public IntStruct[][][] getValue() {
		return list.clone();
	}

	@Override
	public String getValidationValue() {
		return validationValue;
	}

	@Override
	public String getStringTestValue() {
		String string = "[";
			for (IntStruct[][] l1 : list) {
				string += "[";
				for (IntStruct[] l2 : l1) {
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
		return list.length == 0;
	}
}
