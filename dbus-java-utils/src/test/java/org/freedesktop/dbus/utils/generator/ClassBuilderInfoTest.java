package org.freedesktop.dbus.utils.generator;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassBuilderInfoTest {

    @Test
    void testGetSimpleTypeClasses() {
        assertEquals("Class1", ClassBuilderInfo.getSimpleTypeClasses("Class1"));
        assertEquals("Class1", ClassBuilderInfo.getSimpleTypeClasses("com.example.Class1"));
        assertEquals("Map<Class1, List<String>>", ClassBuilderInfo.getSimpleTypeClasses("java.util.Map<com.example.Class1, java.util.List<java.lang.String>>"));
        assertEquals("Map<Class1, List<String>>", ClassBuilderInfo.getSimpleTypeClasses("java.util.Map<Class1, java.util.List<java.lang.String>>"));
        assertEquals("Map<Class1, List<String>>", ClassBuilderInfo.getSimpleTypeClasses("Map<com.example.Class1, java.util.List<java.lang.String>>"));
        assertEquals("Map<Class1, List<String>>", ClassBuilderInfo.getSimpleTypeClasses("java.util.Map<com.example.Class1, List<String>>"));
        assertEquals("Map<?, List<?>>", ClassBuilderInfo.getSimpleTypeClasses("java.util.Map<?, java.util.List<?>>"));
    }

    @Test
    void testGetImportsForType() {
        Set<String> imports;
        imports = ClassBuilderInfo.getImportsForType("com.example.Class1");
        assertEquals(1, imports.size());
        assertTrue(imports.contains("com.example.Class1"));

        // class in the same package, nothing to import
        imports = ClassBuilderInfo.getImportsForType("Class1");
        assertEquals(0, imports.size());

        imports = ClassBuilderInfo.getImportsForType("java.lang.String");
        assertEquals(0, imports.size());

        imports = ClassBuilderInfo.getImportsForType("java.util.Map<com.example.Class1, java.util.List<java.lang.String>>");
        assertEquals(3, imports.size());
        assertTrue(imports.contains("java.util.Map"));
        assertTrue(imports.contains("com.example.Class1"));
        assertTrue(imports.contains("java.util.List"));

        imports = ClassBuilderInfo.getImportsForType("java.util.Map<?, java.util.List<?>>");
        assertEquals(2, imports.size());
        assertTrue(imports.contains("java.util.Map"));
        assertTrue(imports.contains("java.util.List"));

        imports = ClassBuilderInfo.getImportsForType("java.util.List<Class1>");
        assertEquals(1, imports.size());
        assertTrue(imports.contains("java.util.List"));
    }
}
