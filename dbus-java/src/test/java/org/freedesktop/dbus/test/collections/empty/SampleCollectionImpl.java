/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/
package org.freedesktop.dbus.test.collections.empty;

import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructPrimative;
import org.freedesktop.dbus.test.collections.empty.structs.DeepArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepListStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.IEmptyCollectionStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructPrimative;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructPrimative;

public class SampleCollectionImpl implements ISampleCollectionInterface {

	@Override
	public String testListPrimative(ListStructPrimative param) {
		return testValue(param);
	}

	@Override
	public String testListIntStruct(ListStructStruct param) {
		return testValue(param);
	}

	@Override
	public String testDeepList(DeepListStruct param) {
		return testValue(param);	
	}

	@Override
	public String testArrayPrimative(ArrayStructPrimative param) {
		return testValue(param);

	}

	@Override
	public String testArrayIntStruct(ArrayStructIntStruct param) {
		return testValue(param);
	}

	@Override
	public String testDeepArray(DeepArrayStruct param) {
		return testValue(param);
	}

	@Override
	public String testMapPrimative(MapStructPrimative param) {
		return testValue(param);
	}

	@Override
	public String testMapIntStruct(MapStructIntStruct param) {
		return testValue(param);
	}

	@Override
	public String testDeepMap(DeepMapStruct param) {
		return testValue(param);
	}

	@Override
	public String testMixedListMap(ListMapStruct param) {
		return testValue(param);
	}

	@Override
	public String testMixedMapArray(MapArrayStruct param) {
		return testValue(param);
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public String getObjectPath() {
		return "/org/dbus/test/EmptyCollections";
	}

	private String testValue(IEmptyCollectionStruct<?> param) {
		if (param.getValue() == null) {
			throw new IllegalArgumentException("Incorrect param value");
		}
		return param.isEmpty() ? param.getValidationValue() : param.getStringTestValue();		
	}
	
}