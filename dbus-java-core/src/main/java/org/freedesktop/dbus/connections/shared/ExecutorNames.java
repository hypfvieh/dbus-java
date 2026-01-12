package org.freedesktop.dbus.connections.shared;

/**
 * Enum representing different executor services.
 *
 * @author hypfvieh
 * @version 4.0.1 - 2022-02-02
 */
public enum ExecutorNames {
    SIGNAL("SignalExecutor", "DBus-Signal-Receiver"),
    ERROR("ErrorExecutor", "DBus-Error-Receiver"),
    METHODCALL("MethodCallExecutor", "DBus-MethodCall-Receiver"),
    METHODRETURN("MethodReturnExecutor", "DBus-MethodReturn-Receiver");

    private final String description;
    private final String threadName;

    ExecutorNames(String _name, String _threadName) {
        description = _name;
        threadName = _threadName;
    }

    public String getDescription() {
        return description;
    }

    public String getThreadName() {
        return threadName;
    }

    @Override
    public String toString() {
        return description;
    }
}
