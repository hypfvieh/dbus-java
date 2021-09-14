package org.freedesktop.dbus.bin;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class EmbeddedDBusDaemon implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDBusDaemon.class);

    private final BusAddress address;

    private DBusDaemon daemonThread;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public EmbeddedDBusDaemon(BusAddress _address) {
        address = Objects.requireNonNull(_address, "Address required");
        if (_address.getRawAddress().startsWith("tcp")) {
            String addrStr = _address.getRawAddress().replace(",listen=true", "");
            System.setProperty(AbstractConnection.TCP_ADDRESS_PROPERTY, addrStr);
        }
    }

    public EmbeddedDBusDaemon(String _address) throws DBusException {
        this(new BusAddress(_address));
    }

    /**
     *
     */
    @Override
    public void close() throws IOException {
        closed.set(true);
        if (daemonThread != null) {
            daemonThread.close();
            daemonThread = null;
        }
    }

    public void startInForeground() {
        daemonThread = new DBusDaemon();
        daemonThread.start();

        try {
            listen();
        } catch (IOException ex) {
            if (!closed.get()) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void startInBackground() {
        Thread thread = new Thread(this::startInForeground);
        thread.setName("EmbeddedDBusDaemon-" + address);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((th, ex) -> LOGGER.error("Got uncaught exception", ex));
        thread.start();
    }

    public boolean isRunning() {
        return daemonThread == null ? false : daemonThread.isRunning();
    }

    private void listen() throws IOException {
        if ("unix".equals(address.getType()) || "tcp".equals(address.getType())) {
            startSocket(address);
        } else {
            // not possible because otherwise we could not get an address object
            throw new IllegalArgumentException("Unknown address type: " + address.getType());
        }
    }

    private void startSocket(BusAddress _address) throws IOException {
        try (AbstractTransport transport = TransportFactory.createTransport(_address, AbstractConnection.TCP_CONNECT_TIMEOUT, false)) {
            while (daemonThread.isRunning()) {
                try {
                    SocketChannel s = transport.connect();
                    daemonThread.addSock(s);
                } catch (AuthenticationException _ex) {
                    LOGGER.error("Authentication failed");
                }
            }
        }
    }
}
