package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class PrimitiveUtilsTest extends AbstractBaseTest {

    static Stream<PrimitiveWrapperTestData> createComparePrimitiveWrapperData() {
        return Stream.of(
            new PrimitiveWrapperTestData("Equal types", Boolean.class, Boolean.class, true),
            new PrimitiveWrapperTestData("Equal primitve types", boolean.class, boolean.class, true),
            new PrimitiveWrapperTestData("Equal object and primitive", Boolean.class, boolean.class, true),
            new PrimitiveWrapperTestData("Equal primitive and object", Boolean.class, boolean.class, true),
            new PrimitiveWrapperTestData("Unequal types", Boolean.class, String.class, false),
            new PrimitiveWrapperTestData("Unequal primitive types", int.class, long.class, false),
            new PrimitiveWrapperTestData("Unequal primitive and object", boolean.class, String.class, false),
            new PrimitiveWrapperTestData("Unequal object and primitive", Byte.class, int.class, false)
            );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("createComparePrimitiveWrapperData")
    void testComparePrimitiveWrapper(PrimitiveWrapperTestData _testData) {
        assertEquals(_testData.expected(), PrimitiveUtils.isCompatiblePrimitiveOrWrapper(_testData.clz1(), _testData.clz2()));
    }

    record PrimitiveWrapperTestData(String name, Class<?> clz1, Class<?> clz2, boolean expected) {
        @Override
        public String toString() {
            return name;
        }
    }
}
