package org.freedesktop.dbus.bin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class EmbeddedDBusDaemonTest {

    @Test
    public void test_start_stop() throws Exception {

        String address = DirectConnection.createDynamicTCPSession();
        for (int i = 0; i < 2; i++) {

            // initialize
            EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon();
            daemon.setAddress(address);

            // start the daemon in background to not block the test
            AtomicReference<Exception> exception = new AtomicReference<>();
            Thread daemonThread = new Thread(() -> {
                try {
                    daemon.startInForeground();
                } catch (Exception ex) {
                    exception.set(ex);
                }
            });
            daemonThread.start();

            // give the daemon time to start
            Thread.sleep(1000);
            daemon.close();
            assertEquals(null, exception.get()); // assertEquals() gives a better error message
        }
    }
}
