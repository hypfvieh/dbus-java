package org.freedesktop.dbus.matchrules;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MatchRuleFieldTest extends AbstractBaseTest {

    @Test
    void testMatchArg0Namespace() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getSig() {
                return "s";
            }

            @Override
            public Object[] getParameters() throws DBusException {
                return new Object[] {"/com/example/foo/bar"};
            }

        };

        assertTrue(MatchRuleField.ARG0NAMESPACE.getSingleMatcher().test(msg, "/com/example/foo"));
        assertTrue(MatchRuleField.ARG0NAMESPACE.getSingleMatcher().test(msg, "/com/example/foo/bar"));
        assertFalse(MatchRuleField.ARG0NAMESPACE.getSingleMatcher().test(msg, "/com/example/foobar"));
    }

    @Test
    void testMatchPathNamespace() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getPath() {
                return "/com/example/foo/bar";
            }

        };

        assertTrue(MatchRuleField.PATH_NAMESPACE.getSingleMatcher().test(msg, "/com/example/foo"));
        assertTrue(MatchRuleField.PATH_NAMESPACE.getSingleMatcher().test(msg, "/com/example/foo/bar"));
        assertFalse(MatchRuleField.PATH_NAMESPACE.getSingleMatcher().test(msg, "/com/example/foobar"));
    }

    @Test
    void testMatchMsgType() throws DBusException {
        Message msg = new Message() {

            @Override
            public byte getType() {
                return MessageTypes.SIGNAL.getId();
            }

        };

        assertTrue(MatchRuleField.TYPE.getSingleMatcher().test(msg, "signal"));
        assertFalse(MatchRuleField.TYPE.getSingleMatcher().test(msg, "error"));
    }

    @Test
    void testMatchSender() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getSource() {
                return "org.freedesktop.Hal";
            }

        };

        assertTrue(MatchRuleField.SENDER.getSingleMatcher().test(msg, "org.freedesktop.Hal"));
        assertFalse(MatchRuleField.PATH.getSingleMatcher().test(msg, "org.freedesktop.DBus"));
    }

    @Test
    void testMatchPath() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getPath() {
                return "/org/freedesktop/Hal/Manager";
            }

        };

        assertTrue(MatchRuleField.PATH.getSingleMatcher().test(msg, "/org/freedesktop/Hal/Manager"));
        assertFalse(MatchRuleField.PATH.getSingleMatcher().test(msg, "/org/freedesktop/Hal/ObjectMapper"));
    }

    @Test
    void testMatchMember() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getName() {
                return "NameOwnerChanged";
            }

        };

        assertTrue(MatchRuleField.MEMBER.getSingleMatcher().test(msg, "NameOwnerChanged"));
        assertFalse(MatchRuleField.MEMBER.getSingleMatcher().test(msg, "NameOwner"));
    }

    @Test
    void testMatchInterface() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getInterface() {
                return "org.freedesktop.Interface";
            }

        };

        assertTrue(MatchRuleField.INTERFACE.getSingleMatcher().test(msg, "org.freedesktop.Interface"));
        assertFalse(MatchRuleField.INTERFACE.getSingleMatcher().test(msg, "org.freedesktop.Other"));
    }

    @Test
    void testMatchDestination() throws DBusException {
        Message msg = new Message() {

            @Override
            public String getDestination() {
                return "dest";
            }

        };

        assertTrue(MatchRuleField.DESTINATION.getSingleMatcher().test(msg, "dest"));
        assertFalse(MatchRuleField.DESTINATION.getSingleMatcher().test(msg, "xxx"));
    }

    @ParameterizedTest
    @MethodSource("createArg0123PathTestData")
    void testMatchArg0123Path(List<Object> _results, Map<Integer, String> _matcher, boolean _matchResult) throws DBusException {
        multiArgsTest(MatchRuleField.ARG0123PATH, _results, _matcher, _matchResult);
    }

    @ParameterizedTest
    @MethodSource("createArg0123TestData")
    void testMatchArg0123(List<Object> _results, Map<Integer, String> _matcher, boolean _matchResult) throws DBusException {
        multiArgsTest(MatchRuleField.ARG0123, _results, _matcher, _matchResult);
    }

    private void multiArgsTest(MatchRuleField _field, List<Object> _results, Map<Integer, String> _matcher, boolean _matchResult) throws DBusException {
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

        assertEquals(_matchResult, _field.getMultiMatcher().test(msg, _matcher));
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
            Arguments.arguments(List.of(new DBusPath("/aa/bb/cc")), Map.of(0, "/aa/bb/"), true),

            Arguments.arguments(List.of("/aa/b"), Map.of(0, "/aa/bb/"), false),
            Arguments.arguments(List.of("/aa"), Map.of(0, "/aa/bb/"), false),
            Arguments.arguments(List.of("/aa/bb"), Map.of(0, "/aa/bb/"), false),

            Arguments.arguments(List.of("/aa/bb/", "/foo/"), Map.of(0, "/", 1, "/"), true),
            Arguments.arguments(List.of("/aa/bb/", "/foo/"), Map.of(0, "/aa/", 1, "/foo/"), true)
        );
    }

}
