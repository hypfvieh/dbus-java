package org.freedesktop.dbus.matchrules;

import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.constants.MessageTypes;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiPredicate;

public enum MatchRuleField {
    TYPE((m, s) -> Objects.equals(MessageTypes.getRuleNameById(m.getType()), s), null),
    SENDER((m, s) -> Objects.equals(m.getSource(), s), null),
    INTERFACE((m, s) -> Objects.equals(m.getInterface(), s), null),
    MEMBER((m, s) -> Objects.equals(m.getName(), s), null),
    PATH((m, s) -> Objects.equals(m.getPath(), s), null),
    PATH_NAMESPACE((m, s) -> MatchRuleMatcher.matchPathNamespace(m.getPath(), s), null),
    DESTINATION((m, s) -> Objects.equals(m.getDestination(), s), null),
    ARG0123(null, MatchRuleMatcher::matchArg0123),
    ARG0123PATH(null, MatchRuleMatcher::matchArg0123Path),
    ARG0NAMESPACE(MatchRuleMatcher::matchArg0Namespace, null);

    private final BiPredicate<Message, String> singleMatcher;
    private final BiPredicate<Message, Map<Integer, String>> multiMatcher;

    MatchRuleField(BiPredicate<Message, String> _singleMatcher, BiPredicate<Message, Map<Integer, String>> _multiMatcher) {
        singleMatcher = _singleMatcher;
        multiMatcher = _multiMatcher;
    }

    public Entry<MatchRuleField, String> entryOf(String _val) {
        return _val == null ? null : Map.entry(this, _val);
    }

    public BiPredicate<Message, String> getSingleMatcher() {
        return singleMatcher;
    }

    public BiPredicate<Message, Map<Integer, String>> getMultiMatcher() {
        return multiMatcher;
    }

}
