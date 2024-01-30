package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ListMapStruct extends Struct implements IEmptyCollectionStruct<List<Map<String, IntStruct>>> {

    @Position(0)
    private final List<Map<String, IntStruct>> list;

    @Position(1)
    private final String validationValue;

    public ListMapStruct(List<Map<String, IntStruct>> _list, String _validationValue) {
        this.list = _list;
        this.validationValue = _validationValue;
    }

    @Override
    public List<Map<String, IntStruct>> getValue() {
        return list;
    }

    @Override
    public String getValidationValue() {
        return validationValue;
    }

    @Override
    public String getStringTestValue() {
        return list.stream()
                .map(this::toPrintableMap)
                .collect(Collectors.toList())
                .toString();
    }

    private Map<String, String> toPrintableMap(Map<String, IntStruct> _m) {
        return _m.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toSimpleString()));
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
}
