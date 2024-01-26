package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

import java.util.List;
import java.util.stream.Collectors;

public final class ListStructStruct extends Struct implements IEmptyCollectionStruct<List<IntStruct>> {

    @Position(0)
    private final List<IntStruct> list;

    @Position(1)
    private final String validationValue;

    public ListStructStruct(List<IntStruct> _list, String _validationValue) {
        this.list = _list;
        this.validationValue = _validationValue;
    }

    @Override
    public List<IntStruct> getValue() {
        return list;
    }

    @Override
    public String getValidationValue() {
        return validationValue;
    }

    @Override
    public String getStringTestValue() {
        return list.stream()
                .map(IntStruct::toSimpleString)
                .collect(Collectors.joining(","));
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
}
