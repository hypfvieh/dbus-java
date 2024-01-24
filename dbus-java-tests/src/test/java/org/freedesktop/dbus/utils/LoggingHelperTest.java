package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

public class LoggingHelperTest extends AbstractBaseTest {

    @Test
    void testDeepString() {
        // ensure sorted map and set (therefore Map.of and Set.of cannot be used)
        Map<String, String> testMap = new LinkedHashMap<>();
        testMap.put("mapkey", "mapval");
        testMap.put("mapkey2", "mapval2");

        Set<String> testSet = new LinkedHashSet<>();
        testSet.addAll(List.of("Set", "of", "stuff"));

        Object[] objArr = new Object[] {
            List.of("test", "collection"),
            "String Item",
            1,
            new String[] {"String", "Array"},
            new int[] {10, 20, 30},
            testSet,
            new char[] {'h', 'i'},
            new Long[] {2L, 4L},
            testMap,
            47.11d,
            new boolean[][] {new boolean[] {true}, new boolean[] {false}},
            new Object[] {"String", 5, new byte[] {'a', 'b'}}
        };

        assertEquals("[test, collection], "
            + "String Item, "
            + "1, "
            + "[String, Array], "
            + "[10, 20, 30], "
            + "[Set, of, stuff], "
            + "[h, i], "
            + "[2, 4], "
            + "{mapkey=mapval, mapkey2=mapval2}, "
            + "47.11, "
            + "[[true], [false]], "
            + "[String, 5, [97, 98]]",
            LoggingHelper.arraysVeryDeepString(objArr));
    }

    @ParameterizedTest(name = "[{index}] -> {0}")
    @MethodSource("getTestData")
    void testConvertPrimitiveArray(TestValues _val) {
        assertEquals(_val.expected, LoggingHelper.convertToString(_val.input));
    }

    private static Stream<TestValues> getTestData() {
        return Stream.of(
            new TestValues(new boolean[] {true, false, false, true}, "[true, false, false, true]"),
            new TestValues(new char[] {'a', 'c', 'D', 'C'}, "[a, c, D, C]"),
            new TestValues(new byte[] {'H', 'E', 'L', 'L', 'O'}, "[72, 69, 76, 76, 79]"),
            new TestValues(new int[] {1, 3, 5, 7, 9}, "[1, 3, 5, 7, 9]"),
            new TestValues(new long[] {2L, 4L, 6L, 8L}, "[2, 4, 6, 8]"),
            new TestValues(new float[] {0.1f, 0.2f, 0.3f}, "[0.1, 0.2, 0.3]"),
            new TestValues(new double[] {1.0d, 2.0d, 3.0d}, "[1.0, 2.0, 3.0]")
        );
    }

    private static class TestValues {
        private Object input;
        private String expected;

        TestValues(Object _input, String _expected) {
            input = _input;
            expected = _expected;
        }

        @Override
        public String toString() {
            return input.getClass().getTypeName().replace("[]", "Array") + "Test";
        }

    }

}
