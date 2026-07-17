package org.freedesktop.dbus.test;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Verifies that calling {@link org.freedesktop.dbus.connections.impl.DBusConnection#disconnect()} from within a signal
 * handler does not stall for the full receiving-service shutdown timeout (~10s). The handler runs on the connection's
 * SIGNAL executor; disconnect() must not block awaiting termination of that very pool.
 */
public class DisconnectFromSignalHandlerTest extends AbstractDBusBaseTest {

    @Test
    void testDisconnectFromSignalHandlerDoesNotStall() throws Exception {
        CountDownLatch handlerDone = new CountDownLatch(1);
        AtomicLong disconnectMillis = new AtomicLong(-1);

        clientconn.addSigHandler(StallSignalService.StallSignal.class, s -> {
            long start = System.nanoTime();
            clientconn.disconnect(); // called from within the SIGNAL executor thread
            disconnectMillis.set((System.nanoTime() - start) / 1_000_000);
            handlerDone.countDown();
        });

        // trigger the signal from the server side
        serverconn.sendMessage(new StallSignalService.StallSignal(getTestObjectPath()));

        // with the fix the handler returns quickly; without it disconnect() blocks ~10s awaiting its own SIGNAL pool
        assertTimeoutPreemptively(Duration.ofSeconds(6), () ->
            assertTrue(handlerDone.await(6, TimeUnit.SECONDS), "signal handler did not finish disconnect() in time"));

        assertTrue(disconnectMillis.get() >= 0, "disconnect() was not measured");
        assertTrue(disconnectMillis.get() < 5000,
            "disconnect() from signal handler stalled: " + disconnectMillis.get() + " ms");
    }

    @DBusInterfaceName("org.freedesktop.dbus.test.StallSignalService")
    public interface StallSignalService extends DBusInterface {
        class StallSignal extends DBusSignal {
            public StallSignal(String _path) throws DBusException {
                super(_path);
            }
        }
    }
}
