package org.freedesktop.dbus.bin;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.sasl.AuthenticationException;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class EmbeddedDBusDaemon implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDBusDaemon.class);

    private BusAddress address;

    private DBusDaemon daemonThread;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     *
     */
    @Override
    public void close() throws IOException {
        this.closed.set(true);
        if (daemonThread != null) {
            daemonThread.close();
            daemonThread.dbusServer.interrupt();
            daemonThread.sender.interrupt();
            daemonThread = null;
        }
    }

    public void startInForeground() {

        Objects.requireNonNull(address, "busAddress not set");

        daemonThread = new DBusDaemon();
        daemonThread.start();
        daemonThread.sender.start();
        daemonThread.dbusServer.start();

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

    private void listen() throws IOException {
        if ("unix".equals(address.getType()) || "tcp".equals(address.getType())) {
            startSocket(address);
        } else {
            // not possible because otherwise we could not get an address object
            throw new IllegalArgumentException("Unknown address type: " + address.getType());
        }
    }

    private void startSocket(BusAddress address) throws IOException {
        try (AbstractTransport transport = TransportFactory.createTransport(address)) {
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

    public void setAddress(BusAddress address) {
        this.address = address;
    }

    public void setAddress(String address) throws DBusException {
        setAddress(new BusAddress(address));
    }

}
