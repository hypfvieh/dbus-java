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

    String testListPrimitive(ListStructPrimitive _param);

    String testListIntStruct(ListStructStruct _param);

    String testDeepList(DeepListStruct _param);

    String testArrayPrimitive(ArrayStructPrimitive _param);

    String testArrayIntStruct(ArrayStructIntStruct _param);

    String testDeepArray(DeepArrayStruct _param);

    String testMapPrimitive(MapStructPrimitive _param);

    String testMapIntStruct(MapStructIntStruct _param);

    String testDeepMap(DeepMapStruct _param);

    String testMixedListMap(ListMapStruct _param);

    String testMixedMapArray(MapArrayStruct _param);

}
