package org.freedesktop.dbus.test;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.test.helper.interfaces.SlowInterface;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that a registered async callback is notified via handleError (and removed) when the connection is
 * disconnected while the call is still pending.
 */
public class DisconnectCallbackTest extends AbstractDBusBaseTest {

    @Test
    void testPendingCallbackNotifiedOnDisconnect() throws Exception {
        CountDownLatch serverEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch errorCalled = new CountDownLatch(1);

        String path = getTestObjectPath() + "Slow";
        SlowObject obj = new SlowObject(path, serverEntered, release);
        serverconn.exportObject(path, obj);

        SlowInterface remote = clientconn.getRemoteObject(getTestBusName(), path, SlowInterface.class, false);

        clientconn.callWithCallback(remote, "slowCall", new CallbackHandler<String>() {
            @Override
            public void handle(String _r) {
                // not expected in this test
            }

            @Override
            public void handleError(DBusExecutionException _ex) {
                errorCalled.countDown();
            }
        });

        // make sure the server is inside the (blocking) call, so the reply is definitely still pending
        assertTrue(serverEntered.await(10, TimeUnit.SECONDS), "server did not enter slow call");

        try {
            // disconnect while the call is pending -> the callback must be failed via handleError
            clientconn.disconnect();
            assertTrue(errorCalled.await(10, TimeUnit.SECONDS), "handleError was not invoked on disconnect");
        } finally {
            release.countDown(); // let the server method finish
            // the server now replies to the already-gone client, so the daemon bounces a ServiceUnknown error
            // back to the server; consume that expected error so the strict base tearDown does not trip over it
            long deadline = System.currentTimeMillis() + 5000;
            while (serverconn.getError() == null && System.currentTimeMillis() < deadline) {
                TimeUnit.MILLISECONDS.sleep(25);
            }
        }
    }

    public static class SlowObject implements SlowInterface {
        private final String         objectPath;
        private final CountDownLatch serverEntered;
        private final CountDownLatch release;

        SlowObject(String _objectPath, CountDownLatch _serverEntered, CountDownLatch _release) {
            objectPath = _objectPath;
            serverEntered = _serverEntered;
            release = _release;
        }

        @Override
        public String slowCall() {
            serverEntered.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException _ex) {
                Thread.currentThread().interrupt();
            }
            return "done";
        }

        @Override
        public String getObjectPath() {
            return objectPath;
        }
    }
}
