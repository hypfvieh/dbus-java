package org.freedesktop.dbus.matchrules;

import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

public class DBusMatchRuleBuilderTest  extends AbstractBaseTest {

    @Test
    void testEmptyBuild() {
        assertThrows(IllegalStateException.class, () -> DBusMatchRuleBuilder.create().build());
    }

    @Test
    void testArgTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> DBusMatchRuleBuilder.create().withArg0123(100, getShortTestMethodName()).build());
        assertThrows(IllegalArgumentException.class, () -> DBusMatchRuleBuilder.create().withArg0123(-1, getShortTestMethodName()).build());
    }

    @Test
    void testInvalidCombination() {
        assertThrows(IllegalArgumentException.class, () ->
        DBusMatchRuleBuilder.create().withPath("xx").withPathNamespace("y").build());
        assertThrows(IllegalArgumentException.class, () ->
        DBusMatchRuleBuilder.create().withPathNamespace("!").withPath("?").build());
    }

    @Test
    void testBuild() {
        DBusMatchRule dBusMatchRule = DBusMatchRuleBuilder.create()
            .withType(MessageTypes.ERROR)
            .withDestination("foo")
            .withInterface("x.y")
            .withSender("sen.der")
            .build();

        assertEquals(MessageTypes.ERROR.getMatchRuleName(), dBusMatchRule.getMessageType());
        assertEquals("foo", dBusMatchRule.getDestination());
        assertEquals("x.y", dBusMatchRule.getInterface());
        assertEquals("sen.der", dBusMatchRule.getSender());
    }

}
