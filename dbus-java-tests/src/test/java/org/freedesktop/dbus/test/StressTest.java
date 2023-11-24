package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class StressTest extends AbstractDBusDaemonBaseTest {

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
        threads.forEach(t -> runUnchecked(() -> t.join(20_000)));

        // assert
        assertTrue(asyncExceptions.isEmpty(), "No exceptions expected");
        assertEquals(numberOfClients, clientCalls.size());
        for (DBusConnection clientConnection : clientConnections) {
            AtomicInteger counter = clientCalls.get(clientConnection.getUniqueName());
            assertNotNull(counter);
            assertEquals(numberOfRequestsPerClient, counter.get());
        }
    }

    private List<Thread> createClientThreads(List<DBusConnection> _clientConnections, List<DBusConnection> _serviceConnections, int _numberOfRequestsPerClient) {
        List<Thread> threads = new ArrayList<>();
        for (DBusConnection clientConnection : _clientConnections) {
            Thread thread = new Thread(() -> stressServices(clientConnection, _serviceConnections, _numberOfRequestsPerClient));
            thread.setName("ClientThread-" + clientConnection.getUniqueName());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) -> asyncExceptions.add(e));
            threads.add(thread);
        }
        return threads;
    }

    private List<DBusConnection> createClientConnections(int _numberOfClients) throws DBusException {
        List<DBusConnection> clientConnections = new ArrayList<>(_numberOfClients);
        for (int i = 0; i < _numberOfClients; i++) {
            clientConnections.add(DBusConnectionBuilder.forSessionBus().withShared(false).build());
        }
        return clientConnections;
    }

    /**
     *
     */
    private List<DBusConnection> createServices(int _numberOfServices) throws DBusException {
        List<DBusConnection> serviceConnections = new ArrayList<>();
        for (int i = 0; i < _numberOfServices; i++) {
            RemoteObjectImpl service = new RemoteObjectImpl();
            DBusConnection serviceConnection = DBusConnectionBuilder.forSessionBus().withShared(false).build();
            closeables.add(serviceConnection);
            serviceConnections.add(serviceConnection);
            serviceConnection.exportObject(OBJECT_PATH, service);
        }
        return serviceConnections;
    }

    /**
     *
     */
    private void stressServices(DBusConnection _client, List<DBusConnection> _serviceConnections, int _requestCount) {

        // get the client stubs for each service
        List<RemoteObject> services = new ArrayList<>();
        _serviceConnections.forEach(connection -> runUnchecked(() -> {
            RemoteObject service = _client.getRemoteObject(connection.getUniqueName(), OBJECT_PATH, RemoteObject.class);
            services.add(service);
        }));

        // make stress
        for (int i = 0; i < _requestCount; i++) {
            int serviceIdx = random.nextInt(services.size());
            services.get(serviceIdx).doSomething(_client.getUniqueName());
        }
    }

    private static void runUnchecked(ThrowingRunnable _runnable) {
        try {
            _runnable.run();
        } catch (RuntimeException | Error _ex) {
            throw _ex;
        } catch (Throwable _ex) {
            throw new RuntimeException(_ex);
        }
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     *
     */
    public interface RemoteObject extends DBusInterface {

        void doSomething(String _clientName);
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
        public void doSomething(String _clientName) {
            AtomicInteger counter;
            synchronized (clientCalls) {
                counter = clientCalls.get(_clientName);
                if (counter == null) {
                    counter = new AtomicInteger(0);
                    clientCalls.put(_clientName, counter);
                }
            }
            counter.incrementAndGet();
        }
    }
}
