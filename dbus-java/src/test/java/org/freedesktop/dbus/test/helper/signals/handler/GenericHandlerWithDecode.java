package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


public class GenericHandlerWithDecode implements DBusSigHandler<DBusSignal> {

    private final UInt32 expectedIntResult;
    private final String expectedStringResult;

    private Object[] parameters;

    public GenericHandlerWithDecode( UInt32 _expectedIntResult, String _expectedStringResult ){
        expectedIntResult = _expectedIntResult;
        expectedStringResult = _expectedStringResult;
    }

    @Override
    public void handle(DBusSignal s) {
        try{
            parameters = s.getParameters();
        }catch( DBusException ex ){
            fail( "Unexpected DBusException", ex );
        }
    }

    public UInt32 getExpectedIntResult() {
        return expectedIntResult;
    }

    public String getExpectedStringResult() {
        return expectedStringResult;
    }

    public void incomingSameAsExpected(){
        assertNotNull( parameters );
        assertEquals( parameters.length, 2 );

        if (expectedIntResult != null) {
            assertEquals((UInt32)parameters[0], getExpectedIntResult(), "Retrieved int does not match.");
        }
        if (expectedStringResult != null) {
            assertEquals((String)parameters[1], getExpectedStringResult(), "Retrieved string does not match.");
        }
    }

}
