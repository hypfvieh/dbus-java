package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;

public class GenericSignalHandler implements DBusSigHandler<DBusSignal> {

    private int testRuns;

    public GenericSignalHandler(){
        testRuns = 0;
    }

    @Override
    public void handle(DBusSignal s) {
        testRuns++;
        System.out.println( "GenericSignalHandler called" );
    }

    public int getActualTestRuns(){
        return testRuns;
    }
}
