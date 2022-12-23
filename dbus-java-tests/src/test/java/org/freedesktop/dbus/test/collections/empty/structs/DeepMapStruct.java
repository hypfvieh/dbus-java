package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

import java.util.Map;
import java.util.Map.Entry;

public final class DeepMapStruct extends Struct implements IEmptyCollectionStruct<Map<String, Map<String, Map<String, IntStruct>>>> {

    @Position(0)
    private final Map<String, Map<String, Map<String, IntStruct>>> map;

    @Position(1)
    private final String validationValue;

    public DeepMapStruct(Map<String, Map<String, Map<String, IntStruct>>> _map, String _validationValue) {
        this.map = _map;
        this.validationValue = _validationValue;
    }

    @Override
    public Map<String, Map<String, Map<String, IntStruct>>> getValue() {
        return map;
    }

    @Override
    public String getValidationValue() {
        return validationValue;
    }

    @Override
    public String getStringTestValue() {
        String string = "{";
            for (Entry<String, Map<String, Map<String, IntStruct>>> es1 : map.entrySet()) {
                string += String.format("%s:", es1.getKey());
                string += "{";
                for (Entry<String, Map<String, IntStruct>> es2 : es1.getValue().entrySet()) {
                    string += String.format("%s:", es2.getKey());
                    string += "{";
                    for (Entry<String, IntStruct> e : es2.getValue().entrySet()) {
                        string += String.format("%s:%s,", e.getKey(), e.getValue().toSimpleString());
                    }
                    string += "},";
                }
                string += "},";
            }
            string += "}";

        return string.replaceAll(",}", "}");

    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
