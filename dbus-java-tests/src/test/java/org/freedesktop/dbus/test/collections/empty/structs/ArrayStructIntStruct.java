package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArrayStructIntStruct extends Struct implements IEmptyCollectionStruct<IntStruct[]> {

    @Position(0)
    private final IntStruct[] list;

    @Position(1)
    private final String validationValue;

    public ArrayStructIntStruct(IntStruct[] _list, String _validationValue) {
        this.list = _list.clone();
        this.validationValue = _validationValue;
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
                .map(IntStruct::toSimpleString)
                .collect(Collectors.joining(","));
    }

    @Override
    public boolean isEmpty() {
        return list.length == 0;
    }

}
