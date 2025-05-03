package org.freedesktop.dbus.matchrules;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.DBusObjects;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builder to configure a {@link DBusMatchRule}.
 *
 * @author hypfvieh
 * @since 5.1.2 - 2025-05-01
 */
public final class DBusMatchRuleBuilder {

    private final Map<MatchRuleField, String> values = new LinkedHashMap<>();
    private final Map<MatchRuleField, Map<Integer, String>> multiValueFields = new LinkedHashMap<>();

    private DBusMatchRuleBuilder() {
    }

    /**
     * Set sender filter.
     * @param _sender sender to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withSender(String _sender) {
        return putOrRemove(MatchRuleField.SENDER, _sender);
    }

    /**
     * Set destination filter.
     * @param _destination destination to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withDestination(String _destination) {
        return putOrRemove(MatchRuleField.DESTINATION, _destination);
    }

    /**
     * Set path filter.
     * @param _path path to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withPath(String _path) {
        if (values.get(MatchRuleField.PATH_NAMESPACE) != null && _path != null) {
            throw new IllegalArgumentException("Path and PathNamespace cannot be set at the same time");
        }

        return putOrRemove(MatchRuleField.PATH, _path);
    }

    /**
     * Set interface filter.
     * @param _interface interface to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withInterface(String _interface) {
        return putOrRemove(MatchRuleField.INTERFACE, _interface);
    }

    /**
     * Set member/name filter.
     * @param _member member/name to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withMember(String _member) {
        return putOrRemove(MatchRuleField.MEMBER, _member);
    }

    /**
     * Set message type filter.
     * @param _type message type to filter, {@code null} to remove
     * @return this
     * @throws IllegalArgumentException when invalid message type is given
     */
    public DBusMatchRuleBuilder withType(String _type) {
        if (Stream.of(MessageTypes.values())
            .map(e -> e.getMatchRuleName())
            .noneMatch(e -> e.equals(_type))) {
            throw new IllegalArgumentException(_type + " is not a valid message type");
        }
        return putOrRemove(MatchRuleField.TYPE, _type);
    }

    /**
     * Set sender filter.
     * @param _sender sender to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withType(MessageTypes _type) {
        if (_type == null) {
            values.remove(MatchRuleField.TYPE);
        } else {
            values.put(MatchRuleField.TYPE, _type.getMatchRuleName());
        }
        return this;
    }

    /**
     * Set message type filter using class.
     * <p>
     * Class must be a DBusInterface compatible or Error/DBusSignal class.
     * </p>
     * @param _clz class to use for setting up filter, {@code null} to remove
     * @return this
     * @throws DBusException when invalid class s given
     */
    @SuppressWarnings("unchecked")
    public DBusMatchRuleBuilder withType(Class<?> _c) throws DBusException {
        if (DBusInterface.class.isAssignableFrom(_c)) {
            putOrRemove(MatchRuleField.INTERFACE, DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_c)));
        } else if (DBusSignal.class.isAssignableFrom(_c)) {
            if (null == _c.getEnclosingClass()) {
                throw new DBusException("Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
            }
            // Don't export things which are invalid D-Bus interfaces
            String interfaceName = DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_c.getEnclosingClass()));
            String signalName = DBusNamingUtil.getSignalName(_c);
            putOrRemove(MatchRuleField.INTERFACE, interfaceName);
            putOrRemove(MatchRuleField.MEMBER, signalName);
            DBusMatchRule.addToTypeMap(interfaceName + '$' + signalName, (Class<? extends DBusSignal>) _c);
            putOrRemove(MatchRuleField.TYPE, MessageTypes.SIGNAL.getMatchRuleName());
        } else if (Error.class.isAssignableFrom(_c) || DBusExecutionException.class.isAssignableFrom(_c)) {
            putOrRemove(MatchRuleField.INTERFACE, DBusObjects.requireDBusInterface(DBusNamingUtil.getInterfaceName(_c)));
            putOrRemove(MatchRuleField.TYPE, MessageTypes.ERROR.getMatchRuleName());
        } else {
            throw new DBusException("Invalid type for match rule: " + _c);
        }

        return this;
    }

    /**
     * Set/add argument filter.
     * @param _argId argument number
     * @param _arg0123 argument value to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withArg0123(int _argId, String _arg0123) {
        return withArgX(MatchRuleField.ARG0123, _argId, _arg0123);
    }

    /**
     * Set argument path filter.
     * @param _argId argument number
     * @param _arg0123Path argument value to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withArg0123Path(int _argId, String _arg0123Path) {
        return withArgX(MatchRuleField.ARG0123PATH, _argId, _arg0123Path);
    }

    /**
     * Set arg0Namespace filter.
     * @param _arg0Namespace to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withArg0Namespace(String _arg0Namespace) {
        return putOrRemove(MatchRuleField.ARG0NAMESPACE, _arg0Namespace);
    }

    /**
     * Set path namespace filter.
     * @param _pathNamespace to filter, {@code null} to remove
     * @return this
     */
    public DBusMatchRuleBuilder withPathNamespace(String _pathNamespace) {
        if (values.get(MatchRuleField.PATH) != null && _pathNamespace != null) {
            throw new IllegalArgumentException("Path and PathNamespace cannot be set at the same time");
        }
        return putOrRemove(MatchRuleField.PATH_NAMESPACE, _pathNamespace);
    }

    /**
     * Create a new {@link DBusMatchRule} using the configured values of this builder.
     * @return {@link DBusMatchRule}
     */
    public DBusMatchRule build() {
        return new DBusMatchRule(values, multiValueFields);
    }

    /**
     * Set or remove a value for a given field.
     * @param _field field to set
     * @param _val value to set or {@code null} to remove
     * @return this
     */
    private DBusMatchRuleBuilder putOrRemove(MatchRuleField _field, String _val) {
        if (_val == null) {
            values.remove(_field);
        } else {
            values.put(_field, _val);
        }
        return this;
    }

    /**
     * Adds or remove a value of a field at index.
     * @param _field field to add value to
     * @param _argId argument id to add/remove
     * @param _argValue value to set or {@code null} to remove
     * @return this
     */
    private DBusMatchRuleBuilder withArgX(MatchRuleField _field, int _argId, String _argValue) {
        if (_argId > 63 || _argId < 0) {
            throw new IllegalArgumentException("ArgId must be between 0 and 63");
        }

        Map<Integer, String> map = multiValueFields.get(_field);
        if (map != null) {
            if (_argValue == null) {
                map.remove(_argId);
            } else {
                map.put(_argId, _argValue);
            }

            if (map.isEmpty()) {
                multiValueFields.remove(_field);
            }
        } else {
            if (_argValue == null) {
                return this;
            } else {
                multiValueFields.computeIfAbsent(_field, x -> new LinkedHashMap<>()).put(_argId, _argValue);
            }
        }

        return this;
    }

    /**
     * Creates a new builder instance.
     * @return new instance
     */
    public static DBusMatchRuleBuilder create() {
        return new DBusMatchRuleBuilder();
    }

    /**
     * Creates a DBusMatchRule from a map.
     * @param _keyValues values to convert
     * @return DBusMatchRule or {@code null} if input {@code null}
     */
    DBusMatchRule fromMap(Map<String, String> _keyValues) {
        if (_keyValues == null) {
            return null;
        }
        Map<String, String> copy = new HashMap<>(_keyValues);

        Map<String, MatchRuleField> names = Stream.of(MatchRuleField.values())
            .collect(Collectors.toMap(e -> e.name().toLowerCase(Locale.US), e -> e));

        names.forEach((k, v) -> {
            String val = copy.remove(k);
            if (val != null) {
                values.putIfAbsent(v, val);
            }
        });

        if (!copy.isEmpty()) {
            Pattern argPattern = Pattern.compile("^arg([0-9]{1,2})$");
            Pattern argPathPattern = Pattern.compile("^arg([0-9]{1,2})path$");
            for (Entry<String, String> e : copy.entrySet()) {
                Matcher argMatcher = argPattern.matcher(e.getKey());
                Matcher argPathMatcher = argPathPattern.matcher(e.getKey());
                if (argMatcher.matches()) {
                    multiValueFields
                        .computeIfAbsent(MatchRuleField.ARG0123, x -> new LinkedHashMap<>())
                        .putIfAbsent(Integer.valueOf(argMatcher.group(1)), e.getValue());
                }
                if (argPathMatcher.matches()) {
                    multiValueFields
                        .computeIfAbsent(MatchRuleField.ARG0123PATH, x -> new LinkedHashMap<>())
                        .putIfAbsent(Integer.valueOf(argMatcher.group(1)), e.getValue());
                }
            }
        }

        return build();
    }
}
