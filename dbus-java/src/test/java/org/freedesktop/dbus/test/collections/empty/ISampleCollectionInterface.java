/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.collections.empty;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructPrimitive;
import org.freedesktop.dbus.test.collections.empty.structs.DeepArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepListStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructPrimitive;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructPrimitive;

/**
 * A sample remote interface which implements same function for all of the structs
 */
public interface ISampleCollectionInterface extends DBusInterface {

	String testListPrimitive(ListStructPrimitive param);
	
	String testListIntStruct(ListStructStruct param);
	
	String testDeepList(DeepListStruct param);
	
	String testArrayPrimitive(ArrayStructPrimitive param);

	String testArrayIntStruct(ArrayStructIntStruct param);
	
	String testDeepArray(DeepArrayStruct param);
	
	String testMapPrimitive(MapStructPrimitive param);
	
	String testMapIntStruct(MapStructIntStruct param);
	
	String testDeepMap(DeepMapStruct param);
	
	String testMixedListMap(ListMapStruct param);

	String testMixedMapArray(MapArrayStruct param);

}
