package org.freedesktop.dbus.utils;

/**
 * Runnable which allows throwing any exception.
 *
 * @param <T> type of exception which gets thrown
 *
 * @author hypfvieh
 * @since v6.0.0 - 2026-07-05
 */
@FunctionalInterface
public interface IThrowingRunnable<T extends Throwable> {
    /**
     * Returns the result of the supplier or throws an exception.
     *
     * @throws T exception
     */
    void run() throws T;
}
