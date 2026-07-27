package org.freedesktop.dbus.matchrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.freedesktop.dbus.errors.MatchRuleInvalid;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

public class MatchRuleParserTest {

    @Test
    void testConvertRuleWithArgPath() {
        // parsing an argNpath key used to throw (wrong regex group was read) -> now yields the value
        DBusMatchRule rule = MatchRuleParser.convertMatchRule("type='signal',arg0path='/org/example/Foo'");
        assertEquals(Map.of(0, "/org/example/Foo"), rule.getArg0123Path());
    }

    @Test
    void testConvertRuleRejectsTooLargeArgIndex() {
        // spec allows arg indexes 0..63 only; larger indexes must be rejected instead of silently accepted
        assertThrows(MatchRuleInvalid.class, () -> MatchRuleParser.convertMatchRule("type='signal',arg64='x'"));
    }

    @ParameterizedTest
    @MethodSource("createDBusRuleTestData")
    void testParseFromDBusRule(DBusMatchRule _testStr, Map<String, String> _results) {
        Map<String, String> matchRule = MatchRuleParser.parseMatchRule(_testStr.toString());

        assertEquals(_results, matchRule);
    }

    @ParameterizedTest
    @MethodSource("createStringTestData")
    void testParseFromString(String _testStr, Map<String, String> _results) {
        Map<String, String> matchRule = MatchRuleParser.parseMatchRule(_testStr);

        assertEquals(_results, matchRule);
    }

    static Stream<Arguments> createDBusRuleTestData() {
        return Stream.of(
            Arguments.of(DBusMatchRuleBuilder.create().withType(MessageTypes.SIGNAL).build(), Map.of("type", "signal")),
            Arguments.of(DBusMatchRuleBuilder.create()
                .withType(MessageTypes.SIGNAL)
                .withMember("xx'x")
                .withSender("x\\y")
                .build(), Map.of("type", "signal", "member", "xx'x", "sender", "x\\\\y")),
            Arguments.of(DBusMatchRuleBuilder.create()
                .withMember("abc")
                .withArg0123(0, "xyz")
                .withArg0123(1, "f00")
                .withArg0123Path(0, "zzz")
                .withArg0123Path(4, "bla")
                .build(), Map.of("member", "abc", "arg0", "xyz", "arg1", "f00", "arg0path", "zzz", "arg4path", "bla"))
            );
    }

    static Stream<Arguments> createStringTestData() {
        return Stream.of(
            Arguments.of("type='signal',interface='org.example.MyInterface',member='SomethingHappened',"
                + "arg0='/org/example/MyObject',arg0path='/org/example/AnotherObject'",
                Map.of("type", "signal",
                    "interface", "org.example.MyInterface",
                    "member", "SomethingHappened",
                    "arg0", "/org/example/MyObject",
                    "arg0path", "/org/example/AnotherObject"
                    )),
            Arguments.of("type='message',member='Foo'", Map.of("type", "message", "member", "Foo")),
            Arguments.of("type='signal',interface='org.example.MyInterface',arg2='with\\'quote',arg0='some.value'",
                Map.of("type", "signal",
                    "interface", "org.example.MyInterface",
                    "arg2", "with'quote",
                    "arg0", "some.value")),
            Arguments.of("type='signal',interface='org.freedesktop.dbus.test.helper.interfaces.Binding.SampleSignals',member='Triggered'",
                Map.of("type", "signal",
                    "interface", "org.freedesktop.dbus.test.helper.interfaces.Binding.SampleSignals",
                    "member", "Triggered"))
            );
    }
}
