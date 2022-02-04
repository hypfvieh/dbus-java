package org.freedesktop.dbus.utils.bin;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class StructStruct {
    // CHECKSTYLE:OFF
    public String name;
    public String pack;
    // CHECKSTYLE:ON

    public static Map<StructStruct, Type[]> fillPackages(Map<StructStruct, Type[]> _structs, String _pack) {
        Map<StructStruct, Type[]> newmap = new HashMap<>();
        for (StructStruct ss : _structs.keySet()) {
            Type[] type = _structs.get(ss);
            if (null == ss.pack) {
                ss.pack = _pack;
            }
            newmap.put(ss, type);
        }
        return newmap;
    }

    StructStruct(String _name) {
        this.name = _name;
    }

    StructStruct(String _name, String _pack) {
        this.name = _name;
        this.pack = _pack;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object _o) {
        if (!(_o instanceof StructStruct)) {
            return false;
        }
        if (!name.equals(((StructStruct) _o).name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "<" + name + ", " + pack + ">";
    }
}
