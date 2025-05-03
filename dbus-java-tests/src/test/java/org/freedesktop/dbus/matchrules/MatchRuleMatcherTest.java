package org.freedesktop.dbus.matchrules;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

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

        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getSig()).thenReturn(dBusType);
        Mockito.when(msg.getParameters()).thenReturn(_results.toArray());
        assertEquals(_matchResult, MatchRuleMatcher.matchArg0123Path(msg, _matcher));
    }

    @ParameterizedTest
    @MethodSource("createArg0123TestData")
    void testMatchArg0123(List<Object> _results, Map<Integer, String> _matcher, boolean _matchResult) throws DBusException {

        Type[] dataTypes = _results.stream().map(e -> (Type) e.getClass()).toArray(Type[]::new);
        String dBusType = Marshalling.getDBusType(dataTypes);

        Message msg = Mockito.mock(Message.class);
        Mockito.when(msg.getSig()).thenReturn(dBusType);
        Mockito.when(msg.getParameters()).thenReturn(_results.toArray());

        assertEquals(_matchResult, MatchRuleMatcher.matchArg0123(msg, _matcher));
    }

    static Stream<Arguments> createArg0123TestData() {
        return Stream.of(
            Arguments.arguments(List.of("test"), Map.of(0, "test"), true),
            Arguments.arguments(List.of("test", 1), Map.of(0, "foo"), false),
            Arguments.arguments(List.of("test"), Map.of(0, "te"), false)
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
