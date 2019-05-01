package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;

/**
 * Base class for all signals which are tested.
 * @author hypfvieh
 * @since v3.0.0 - 2018-02-26
 */
public abstract class AbstractSignalHandler<T extends DBusSignal> implements DBusSigHandler<T> {
    private final AtomicInteger testRuns = new AtomicInteger(0);

    private final int expectedRuns;

    public AbstractSignalHandler(int _expectedRuns) {
        expectedRuns = _expectedRuns;
    }

    /** Implemented by subclasses */
    protected abstract void handleImpl(T _s);

    /** Check that we do no run to often, then call handleImpl to do the real work. */
    @Override
    public final void handle(T _s) { // should not be implemented by subclasses
        getTestRuns().incrementAndGet();
        assertTrue(getExpectedRuns() <= getActualTestRuns(), "Signal received to often.");

        System.out.println(getClass().getSimpleName() + " running");

        handleImpl(_s);
    }

    protected AtomicInteger getTestRuns() {
        return testRuns;
    }

    public int getActualTestRuns() {
        return testRuns.get();
    }

    public int getExpectedRuns() {
        return expectedRuns;
    }

}
