package org.freedesktop.dbus.test.helper.signals.handler;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.slf4j.LoggerFactory;

public class GenericSignalHandler implements DBusSigHandler<DBusSignal> {

    private int testRuns;

    public GenericSignalHandler() {
        testRuns = 0;
    }

    @Override
    public void handle(DBusSignal _s) {
        testRuns++;
        LoggerFactory.getLogger(getClass()).debug("GenericSignalHandler called");
    }

    public int getActualTestRuns() {
        return testRuns;
    }
}

