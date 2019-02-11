package org.freedesktop.dbus.bin;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;

/**
 *
 */
public class EmbeddedDBusDaemon implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDBusDaemon.class);

    private BusAddress address;

    private DBusDaemon daemonThread;

    private int authTypes = SASL.AUTH_EXTERNAL;

    private Closeable listenSocket;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     *
     */
    @Override
    public void close() throws IOException {
        this.closed.set(true);
        if (listenSocket != null) {
            listenSocket.close();
            listenSocket = null;
        }
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

        if ("unix".equals(address.getType())) {
            startUnixSocket(address);
        } else if ("tcp".equals(address.getType())) {
            startTCPSocket(address);
        } else {
            // not possible because otherwise we could not get an address object
            throw new IllegalArgumentException("Unknown address type: " + address.getType());
        }
    }

    private void startUnixSocket(BusAddress address) throws IOException {
        LOGGER.debug("enter");
        UnixServerSocket uss;
        if (address.isAbstract()) {
            uss = new UnixServerSocket(new UnixSocketAddress(address.getAbstract(), true));
        } else {
            uss = new UnixServerSocket(new UnixSocketAddress(address.getPath(), false));
        }
        listenSocket = uss;

        // accept new connections
        while (daemonThread.isRunning()) {
            UnixSocket s = uss.accept();
            if ((new SASL()).auth(SASL.SaslMode.SERVER, authTypes, address.getGuid(), s.getOutputStream(), s.getInputStream(), s)) {
                // s.setBlocking(false);
                daemonThread.addSock(s);
            } else {
                s.close();
            }
        }
        uss.close();
        LOGGER.debug("exit");

    }

    private void startTCPSocket(BusAddress address) throws IOException {

        LOGGER.debug("enter");

        try (ServerSocket ss = new ServerSocket(address.getPort(), 10, InetAddress.getByName(address.getHost()))) {
            listenSocket = ss;

            // accept new connections
            while (daemonThread.isRunning()) {
                Socket s = ss.accept();
                boolean authOK = false;
                try {
                    authOK = (new SASL()).auth(SASL.SaslMode.SERVER, authTypes, address.getGuid(), s.getOutputStream(), s.getInputStream(), null);
                } catch (Exception e) {
                    LOGGER.debug("", e);
                }
                if (authOK) {
                    daemonThread.addSock(s);
                } else {
                    s.close();
                }
            }
            LOGGER.debug("exit");
        }
    }

    public void setAddress(BusAddress address) {
        this.address = address;
    }

    public void setAddress(String address) throws DBusException {
        setAddress(new BusAddress(address));
    }

    public void setAuthTypes(int authTypes) {
        this.authTypes = authTypes;
    }
}
