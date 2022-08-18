package org.freedesktop.dbus.utils;

import java.util.Arrays;

/**
 * Helper for some logging stuff, e.g. avoid call {@link Arrays#deepToString(Object[])} if loglevel is not enabled.
 *
 * @author David M.
 * @since v3.2.4 - 2020-08-24
 */
public final class LoggingHelper {

    private LoggingHelper() {
    }

    /**
     * Calls {@link Arrays#deepToString(Object[])} if given boolean is true, returns null otherwise.
     *
     * @param _loggingEnabled true to create deep-string representation of given array
     * @param _array array to convert to deep-string
     *
     * @return String or null if given boolean is false or _array is null
     */
    public static String arraysDeepString(boolean _loggingEnabled, Object[] _array) {
        if (!_loggingEnabled) {
            return null;
        }

        if (_array == null) {
            return null;
        }

        return Arrays.deepToString(_array);
    }

    /**
     * Executes the runnable if the boolean is true.
     *
     * @param _enabled boolean, if true runnable is executed
     * @param _loggerCall runnable containing logger call
     */
    public static void logIf(boolean _enabled, Runnable _loggerCall) {
        if (_enabled) {
            _loggerCall.run();
        }
    }

}
