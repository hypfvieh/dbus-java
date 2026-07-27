package org.freedesktop.dbus.matchrules;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class MatchRuleMatcherTest extends AbstractBaseTest {

    @ParameterizedTest
    @MethodSource("createArg0123PathTestData")
    void testMatchArg0123Path(List<Object> _results, Map<Integer, String> _matcher, boolean _matchResult) throws DBusException {
        Type[] dataTypes = _results.stream().map(e -> (Type) e.getClass()).toArray(Type[]::new);
        String dBusType = Marshalling.getDBusType(dataTypes);

        Message msg = new Message() {

            @Override
            public String getSig() {
                return dBusType;
            }

            @Override
            public Object[] getParameters() throws DBusException {
                return _results.toArray();
            }

        };

        assertEquals(_matchResult, MatchRuleMatcher.matchArg0123Path(msg, _matcher));
    }

    @ParameterizedTest
    @MethodSource("createArg0123TestData")
    void testMatchArg0123(List<Object> _results, Map<Integer, String> _matcher, boolean _matchResult) throws DBusException {

        Type[] dataTypes = _results.stream().map(e -> (Type) e.getClass()).toArray(Type[]::new);
        String dBusType = Marshalling.getDBusType(dataTypes);

        Message msg = new Message() {

            @Override
            public String getSig() {
                return dBusType;
            }

            @Override
            public Object[] getParameters() throws DBusException {
                return _results.toArray();
            }

        };

        assertEquals(_matchResult, MatchRuleMatcher.matchArg0123(msg, _matcher));
    }

    @Test
    void testMatchPathNamespace() {
        assertTrue(MatchRuleMatcher.matchPathNamespace("/com/example/foo", "/com/example"));
        assertTrue(MatchRuleMatcher.matchPathNamespace("/com/example", "/com/example"));
        assertFalse(MatchRuleMatcher.matchPathNamespace("/com/example/foobar", "/com/example/foo"));
        // an input that is not a valid object path must never match (previously _input was not validated)
        assertFalse(MatchRuleMatcher.matchPathNamespace("/com//example", "/com"));
    }

    static Stream<Arguments> createArg0123TestData() {
        return Stream.of(
            Arguments.arguments(List.of("test"), Map.of(0, "test"), true),
            Arguments.arguments(List.of("test", 1), Map.of(0, "foo"), false),
            Arguments.arguments(List.of("test"), Map.of(0, "te"), false),
            // multiple arg constraints must all match (logical AND)
            Arguments.arguments(List.of("test", "foo"), Map.of(0, "test", 1, "foo"), true),
            Arguments.arguments(List.of("test", "foo"), Map.of(0, "test", 1, "nomatch"), false),
            // arg index beyond the actual parameters must not match
            Arguments.arguments(List.of("a"), Map.of(0, "a", 3, "b"), false)
            );
    }

    static Stream<Arguments> createArg0123PathTestData() {
        return Stream.of(
            Arguments.arguments(List.of("/aa/bb/"), Map.of(0, "/"), true),
            Arguments.arguments(List.of("/aa/bb/"), Map.of(0, "/aa/"), true),
            Arguments.arguments(List.of("/aa/bb/"), Map.of(0, "/aa/bb/"), true),

            Arguments.arguments(List.of("/aa/bb/cc/"), Map.of(0, "/aa/bb/"), true),
            Arguments.arguments(List.of("/aa/bb/cc"), Map.of(0, "/aa/bb/"), true),

            Arguments.arguments(List.of("/aa/b"), Map.of(0, "/aa/bb/"), false),
            Arguments.arguments(List.of("/aa"), Map.of(0, "/aa/bb/"), false),
            Arguments.arguments(List.of("/aa/bb"), Map.of(0, "/aa/bb/"), false),

            Arguments.arguments(List.of("/aa/bb/", "/foo/"), Map.of(0, "/", 1, "/"), true),
            Arguments.arguments(List.of("/aa/bb/", "/foo/"), Map.of(0, "/aa/", 1, "/foo/"), true)
        );
    }

}
