package org.freedesktop.dbus.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.helper.SampleClass;
import org.junit.jupiter.api.Test;

public class TestDisconnectCallback extends AbstractDBusBaseTest {

    @Test
    public void testDisconnectCallback() throws DBusException, InterruptedException {
        TestCallback callback = new TestCallback();

        DBusConnection serverConnection = DBusConnectionBuilder.forSessionBus().build();
        serverConnection.setDisconnectCallback(callback);
        DBusConnection clientConnection = DBusConnectionBuilder.forSessionBus().build();
        clientConnection.setDisconnectCallback(callback);
        serverConnection.setWeakReferences(true);
        clientConnection.setWeakReferences(true);

        serverConnection.requestBusName("foo.bar.why.again.disconnect.Test");

        SampleClass tclass = new SampleClass(serverConnection);

        serverConnection.exportObject("/Test2001", tclass);

        clientConnection.disconnect();
        Thread.sleep(1000L);
        serverConnection.disconnect();
        Thread.sleep(1000L);
        
        assertEquals(1, callback.clientDisconnectCounter.get());
        assertEquals(0, callback.disconnectErrorCounter.get());
        assertEquals(0, callback.exceptionTerminateCounter.get());
        assertEquals(1, callback.expectedCounter.get(2));
        assertEquals(1, callback.expectedCounter.get(null));
    }
    
    static class TestCallback implements IDisconnectCallback {
        AtomicInteger clientDisconnectCounter = new AtomicInteger(0);
        Map<Integer, Integer> expectedCounter = new HashMap<>();
        AtomicInteger disconnectErrorCounter = new AtomicInteger(0);
        AtomicInteger exceptionTerminateCounter = new AtomicInteger(0);
        
        @Override
        public void clientDisconnect() {
            clientDisconnectCounter.incrementAndGet();
        }
        
        @Override
        public void disconnectOnError(IOException _ex) {
            disconnectErrorCounter.incrementAndGet();
        }
        
        @Override
        public void exceptionOnTerminate(IOException _ex) {
            exceptionTerminateCounter.incrementAndGet();
        }
        
        @Override
        public void requestedDisconnect(Integer _connectionId) {
            Integer integer = expectedCounter.get(_connectionId);
            if (integer == null) {
                expectedCounter.put(_connectionId, 1);
            } else {
                expectedCounter.put(_connectionId, integer++);
            }
        }
    }
}
