package org.freedesktop.dbus.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UtilTest {

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource({
        "Min OK, 1, 3, false",
        "Min Equal, 1, 1, false",
        "Min lower, 5, 1, true"
    })
    void testRequireMinimum(String _name, int _minVal, int _testVal, boolean _throws) {
        if (_throws) {
            assertThrows(IllegalArgumentException.class, () -> Util.requireMinimum(_minVal, _testVal));
        } else {
            int result = assertDoesNotThrow(() -> Util.requireMinimum(_minVal, _testVal));
            assertEquals(_testVal, result);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource({
        "No Snake,This is no snake,This is no snake",
        "Snake,This_is_a_snake,ThisIsASnake",
        "Partial Snake,This_is_partial snake,ThisIsPartial snake",
        "Snake with numbers,This_is_0_8_15_snake,ThisIs0815Snake",
    })
    void testSnakeToCamelCase(String _name, String _input, String _expected) {
        assertEquals(_expected, Util.snakeToCamelCase(_input));
    }
}
