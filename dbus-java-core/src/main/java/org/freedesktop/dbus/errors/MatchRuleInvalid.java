package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

import java.io.Serial;

/**
 * Thrown if the match rule is invalid
 */
public class MatchRuleInvalid extends DBusExecutionException {
    @Serial
    private static final long serialVersionUID = 6922529529288327323L;

    public MatchRuleInvalid(String _message) {
        super(_message);
    }

    public MatchRuleInvalid(String _message, Exception _ex) {
        super(_message, _ex);
    }

}
