package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if the match rule is invalid
 */
@SuppressWarnings("serial")
public class MatchRuleInvalid extends DBusExecutionException {
    public MatchRuleInvalid(String message) {
        super(message);
    }
}