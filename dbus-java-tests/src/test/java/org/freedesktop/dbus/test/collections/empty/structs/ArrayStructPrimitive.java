package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ArrayStructPrimitive extends Struct implements IEmptyCollectionStruct<int[]> {

    @Position(0)
    private final int[] list;

    @Position(1)
    private final String validationValue;

    public ArrayStructPrimitive(int[] _list, String _validationValue) {
        this.list = _list.clone();
        this.validationValue = _validationValue;
    }

    @Override
    public int[] getValue() {
        return list.clone();
    }

    @Override
    public String getValidationValue() {
        return validationValue;
    }

    @Override
    public String getStringTestValue() {
        return IntStream.of(list).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }

    @Override
    public boolean isEmpty() {
        return list.length == 0;
    }

}
