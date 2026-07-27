package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

class TypeConverterTest {

    @Test
    void testNestedGenericsKeepEveryLevel() throws Exception {
        // aaas = List<List<List<String>>> - every nesting level must be preserved (was collapsed to two levels)
        assertEquals("java.util.List<java.util.List<java.util.List<java.lang.CharSequence>>>",
            TypeConverter.getJavaTypeFromDBusType("aaas", new HashSet<>()));
    }

    @Test
    void testMapKeyAndValueTypesAreDistinct() throws Exception {
        // a{asai} = Map<List<String>, List<Integer>> - key and value type must not collapse into one
        assertEquals("java.util.Map<java.util.List<java.lang.CharSequence>, java.util.List<java.lang.Integer>>",
            TypeConverter.getJavaTypeFromDBusType("a{asai}", new HashSet<>()));
    }
}
