package org.freedesktop.dbus.test.collections.empty;

import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ArrayStructPrimitive;
import org.freedesktop.dbus.test.collections.empty.structs.DeepArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepListStruct;
import org.freedesktop.dbus.test.collections.empty.structs.DeepMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.IEmptyCollectionStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListMapStruct;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructPrimitive;
import org.freedesktop.dbus.test.collections.empty.structs.ListStructStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapArrayStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructIntStruct;
import org.freedesktop.dbus.test.collections.empty.structs.MapStructPrimitive;

public class SampleCollectionImpl implements ISampleCollectionInterface {

    @Override
    public String testListPrimitive(ListStructPrimitive _param) {
        return testValue(_param);
    }

    @Override
    public String testListIntStruct(ListStructStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testDeepList(DeepListStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testArrayPrimitive(ArrayStructPrimitive _param) {
        return testValue(_param);

    }

    @Override
    public String testArrayIntStruct(ArrayStructIntStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testDeepArray(DeepArrayStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testMapPrimitive(MapStructPrimitive _param) {
        return testValue(_param);
    }

    @Override
    public String testMapIntStruct(MapStructIntStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testDeepMap(DeepMapStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testMixedListMap(ListMapStruct _param) {
        return testValue(_param);
    }

    @Override
    public String testMixedMapArray(MapArrayStruct _param) {
        return testValue(_param);
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return "/org/dbus/test/EmptyCollections";
    }

    private String testValue(IEmptyCollectionStruct<?> _param) {
        if (_param.getValue() == null) {
            throw new IllegalArgumentException("Incorrect param value");
        }
        return _param.isEmpty() ? _param.getValidationValue() : _param.getStringTestValue();
    }

}
