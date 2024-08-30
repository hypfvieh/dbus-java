package org.freedesktop.dbus.types;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class VariantBuilderTest extends AbstractBaseTest {

    @Test
    void testBuilder() {

        assertEquals(new Variant<>(List.of("str"), "as"),
        VariantBuilder.of(List.class)
            .withGenericTypes(String.class)
            .create(List.of("str")));

        assertEquals(new Variant<>("foo"),
        VariantBuilder.of(String.class)
            .create("foo"));

        assertEquals(new Variant<>(Map.of(1, "str"), "a{is}"),
        VariantBuilder.of(Map.class)
            .withGenericTypes(Integer.class, String.class)
            .create(Map.of(1, "str")));

        assertEquals(new Variant<>(List.of(new VbStruct("test", false)), "a(sb)"),
        VariantBuilder.of(List.class)
            .withGenericTypes(VbStruct.class)
            .create(List.of(new VbStruct("test", false))));

        assertThrows(NullPointerException.class, () -> VariantBuilder.of(String.class).create(null));
        assertThrows(IllegalArgumentException.class, () -> VariantBuilder.of(String.class).create(1));
        assertThrows(NullPointerException.class, () -> VariantBuilder.of(null));

    }

    public static class VbStruct extends Struct {
        @Position(0)
        private final String val1;
        @Position(1)
        private final boolean val2;

        public VbStruct(String _val1, boolean _val2) {
            super();
            val1 = _val1;
            val2 = _val2;
        }

    }
}
