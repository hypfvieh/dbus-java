package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class StressTest {

    private static final String OBJECT_PATH = "/org/freedesktop/dbus/test/RemoteObjectImpl";

    private final List<Closeable> closeables = new ArrayList<>();

    private final Random random = new Random();

    private final Map<String, AtomicInteger> clientCalls = new HashMap<>();

    private final List<Throwable> asyncExceptions = new CopyOnWriteArrayList<>();

    @AfterEach
    public void tearDown() throws Exception {
        Collections.reverse(closeables);
        closeables.forEach(closeable -> runUnchecked(() -> closeable.close()));
    }

    @Test
    public void test() throws Exception {

        int numberOfServices = 10;
        int numberOfClients = 10;
        int numberOfRequestsPerClient = 100;

        // create the services
        List<DBusConnection> serviceConnections = createServices(numberOfServices);

        // create the clients
        List<DBusConnection> clientConnections = createClientConnections(numberOfClients);

        // start the client tests
        List<Thread> threads = createClientThreads(clientConnections, serviceConnections, numberOfRequestsPerClient);
        threads.forEach(Thread::start);
        threads.forEach(t -> runUnchecked(() -> t.join(10_000)));

        // assert
        assertTrue(asyncExceptions.isEmpty(), "No exceptions expected");
        assertEquals(numberOfClients, clientCalls.size());
        for (DBusConnection clientConnection : clientConnections) {
            AtomicInteger counter = clientCalls.get(clientConnection.getUniqueName());
            assertNotNull(counter);
            assertEquals(numberOfRequestsPerClient, counter.get());
        }
    }

    private List<Thread> createClientThreads(List<DBusConnection> clientConnections, List<DBusConnection> serviceConnections, int numberOfRequestsPerClient) {
        List<Thread> threads = new ArrayList<>();
        for (DBusConnection clientConnection : clientConnections) {
            Thread thread = new Thread(() -> stressServices(clientConnection, serviceConnections, numberOfRequestsPerClient));
            thread.setName("ClientThread-" + clientConnection.getUniqueName());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) -> asyncExceptions.add(e));
            threads.add(thread);
        }
        return threads;
    }

    private List<DBusConnection> createClientConnections(int numberOfClients) throws DBusException {
        List<DBusConnection> clientConnections = new ArrayList<>(numberOfClients);
        for (int i = 0; i < numberOfClients; i++) {
            clientConnections.add(DBusConnection.newConnection(DBusBusType.SESSION));
        }
        return clientConnections;
    }

    /**
     *
     */
    private List<DBusConnection> createServices(int numberOfServices) throws DBusException {
        List<DBusConnection> serviceConnections = new ArrayList<>();
        for (int i = 0; i < numberOfServices; i++) {
            RemoteObjectImpl service = new RemoteObjectImpl();
            DBusConnection serviceConnection = DBusConnection.newConnection(DBusBusType.SESSION);
            closeables.add(serviceConnection);
            serviceConnections.add(serviceConnection);
            serviceConnection.exportObject(OBJECT_PATH, service);
        }
        return serviceConnections;
    }

    /**
     *
     */
    private void stressServices(DBusConnection client, List<DBusConnection> serviceConnections, int requestCount) {

        // get the client stubs for each service
        List<RemoteObject> services = new ArrayList<>();
        serviceConnections.forEach(connection -> runUnchecked(() -> {
            RemoteObject service = client.getRemoteObject(connection.getUniqueName(), OBJECT_PATH, RemoteObject.class);
            services.add(service);
        }));

        // make stress
        for (int i = 0; i < requestCount; i++) {
            int serviceIdx = random.nextInt(services.size());
            services.get(serviceIdx).doSomething(client.getUniqueName());
        }
    }

    private static void runUnchecked(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     *
     */
    public static interface RemoteObject extends DBusInterface {

        public void doSomething(String clientName);
    }

    /**
     *
     */
    public class RemoteObjectImpl implements RemoteObject {

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public void doSomething(String clientName) {
            AtomicInteger counter;
            synchronized (clientCalls) {
                counter = clientCalls.get(clientName);
                if (counter == null) {
                    counter = new AtomicInteger(0);
                    clientCalls.put(clientName, counter);
                }
            }
            counter.incrementAndGet();
        }
    }
}
