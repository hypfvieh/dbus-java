package org.freedesktop.dbus.test;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;

import java.util.function.Function;

/**
 * Base class for tests that need a fresh {@link EmbeddedDBusDaemon} on the active transport plus a client connection.
 * <p>
 * Removes the "create dynamic session → start embedded daemon → connect" boilerplate that was duplicated across many
 * transport/daemon tests. Tests with special address requirements (address lists, nonce files, custom bind/family,
 * {@code dir=} listen addresses) intentionally do not use this helper.
 * </p>
 */
public abstract class AbstractEmbeddedDaemonTest extends AbstractBaseTest {

    /**
     * Starts a default {@link EmbeddedDBusDaemon} on a fresh dynamic session of the active transport, opens a
     * non-shared client connection to it and passes it to the given consumer.
     *
     * @param _consumer receives the connected client connection
     * @throws Exception on any failure
     */
    protected void withEmbeddedConnection(ConnectionConsumer _consumer) throws Exception {
        withEmbeddedConnection(EmbeddedDBusDaemon::new, _consumer);
    }

    /**
     * Same as {@link #withEmbeddedConnection(ConnectionConsumer)}, but the daemon is created via the given factory
     * (e.g. {@code DebuggableEmbeddedDBusDaemon::new}).
     *
     * @param _daemonFactory factory creating the daemon for a listening bus address
     * @param _consumer receives the connected client connection
     * @throws Exception on any failure
     */
    protected void withEmbeddedConnection(Function<BusAddress, ? extends EmbeddedDBusDaemon> _daemonFactory,
            ConnectionConsumer _consumer) throws Exception {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);
        BusAddress connectAddress = BusAddress.of(newAddress);
        BusAddress listenAddress = BusAddress.of(newAddress + ",listen=true");

        try (EmbeddedDBusDaemon daemon = _daemonFactory.apply(listenAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);
            try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress).withShared(false).build()) {
                _consumer.accept(conn);
            }
        }
    }

    @FunctionalInterface
    protected interface ConnectionConsumer {
        void accept(DBusConnection _conn) throws Exception;
    }
}
