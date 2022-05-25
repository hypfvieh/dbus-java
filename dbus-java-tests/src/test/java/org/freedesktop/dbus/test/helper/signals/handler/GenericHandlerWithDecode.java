package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.opentest4j.AssertionFailedError;


public class GenericHandlerWithDecode implements DBusSigHandler<DBusSignal> {

    private final UInt32 expectedIntResult;
    private final String expectedStringResult;

    protected AssertionFailedError assertionError;

    private Object[] parameters;

    public GenericHandlerWithDecode( UInt32 _expectedIntResult, String _expectedStringResult ){
        expectedIntResult = _expectedIntResult;
        expectedStringResult = _expectedStringResult;
    }

    @Override
    public void handle(DBusSignal s) {
        try {
            parameters = s.getParameters();
        } catch (DBusException ex) {
            setFailed(false, "Unexpected DBusException", ex);
        }
    }

    public UInt32 getExpectedIntResult() {
        return expectedIntResult;
    }

    public String getExpectedStringResult() {
        return expectedStringResult;
    }

    public Throwable getAssertionError() {
        return assertionError;
    }

    protected void setFailed(boolean _condition, String _message) {
        if (!_condition) {
            assertionError = new AssertionFailedError(_message);
            throw assertionError;
        }
    }

    protected void setFailed(boolean _condition, String _message, Exception _ex) {
        if (!_condition) {
            assertionError = new AssertionFailedError(_message, _ex);
            throw assertionError;
        }
    }

    public void incomingSameAsExpected() {
        setFailed(parameters != null, "No parameters");
        setFailed(parameters.length == 2, "2 parameters expected but " + parameters.length + " found");

        if (expectedIntResult != null) {
            setFailed(parameters[0].equals(getExpectedIntResult()), "Retrieved int does not match.");
        }
        if (expectedStringResult != null) {
            setFailed(parameters[1].equals(getExpectedStringResult()), "Retrieved string does not match.");
        }
    }

}
