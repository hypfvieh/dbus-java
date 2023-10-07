package com.github.hypfvieh.dbus.examples.export;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Example interface used to demonstrate exporting of an object.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-04
 */
public interface ISampleExport extends DBusInterface {
    /**
     * Adds a to b and returns the result.
     *
     * @param _a first number
     * @param _b second number
     * @return sum of both numbers
     */
    int add(int _a, int _b);

    /**
     * Terminate the running application remotely.
     */
    void terminateApp();
}
