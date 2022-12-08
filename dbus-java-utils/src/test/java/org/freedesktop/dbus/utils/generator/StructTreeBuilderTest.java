package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.Test;

class StructTreeBuilderTest {

    @Test
    void testCreateStructContainingStruct() throws DBusException {
        String dbusTypeStr = "a(sa(sv))";
        ClassBuilderInfo classBuilderInfo = new ClassBuilderInfo();
        classBuilderInfo.setClassName("DummyClass");
        classBuilderInfo.setPackageName("unit.test");

        List<ClassBuilderInfo> generated = new ArrayList<>();
        new StructTreeBuilder()
            .buildStructClasses(dbusTypeStr, "UnitTestStruct", classBuilderInfo, generated);

        assertEquals(2, generated.size());
        assertEquals("UnitTestStruct", generated.get(0).getClassName());
        assertEquals("UnitTestStructStruct", generated.get(1).getClassName());

        assertTrue(generated.get(0).createClassFileContent().contains("private final String member0"));
        assertTrue(generated.get(0).createClassFileContent().contains("private final List<UnitTestStructStruct> member1;"));
    }

}
