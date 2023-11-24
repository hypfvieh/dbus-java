package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.CallbackHandler;

import java.util.List;

public final class NestedListCallbackHandler implements CallbackHandler<List<List<Integer>>> {
    private List<List<Integer>> retval;

    @Override
    public void handle(List<List<Integer>> _r) {
        retval = _r;
    }

    @Override
    public void handleError(DBusExecutionException _e) {
    }

    public List<List<Integer>> getRetval() {
        return retval;
    }
}
