package org.freedesktop.dbus.bin;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder.SaslAuthMode;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple DBusDaemon implementation to use if no DBusDaemon is running on the OS level.
 */
public class EmbeddedDBusDaemon implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDBusDaemon.class);

    private final BusAddress address;

    private DBusDaemon daemonThread;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private SaslAuthMode saslAuthMode;

    private String unixSocketFileOwner;

    private String unixSocketFileGroup;

    private PosixFilePermission[] unixSocketFilePermissions;

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
     * Shutdown the running DBusDaemon instance.
     */
    @Override
    public void close() throws IOException {
        closed.set(true);
        if (daemonThread != null) {
            daemonThread.close();
            daemonThread = null;
        }
    }

    /**
     * Run the DBusDaemon in foreground.
     * <p>
     * This is a blocking operation.
     */
    public void startInForeground() {
        daemonThread = new DBusDaemon();
        daemonThread.start();

        try {
            startListening();
        } catch (IOException | DBusException ex) {
            if (!closed.get()) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Start the DBusDaemon in background and returns immediately.
     * <p>
     * This method may return before the background thread is ready.
     * To ensure the the background thread is running on return use {@link #startInBackgroundAndWait(long)}.
     */
    public void startInBackground() {
        Thread thread = new Thread(this::startInForeground);
        thread.setName("EmbeddedDBusDaemon-" + address);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((th, ex) -> LOGGER.error("Got uncaught exception", ex));
        thread.start();
    }

    /**
     * Starts the DBusDaemon in background.
     * <p>
     * Will wait up to the given period of milliseconds for the background thread to get ready.
     * If given wait time exceeded, a {@link RuntimeException} is thrown.
     *
     * @param _maxWaitMillis maximum wait time in milliseconds
     */
    public void startInBackgroundAndWait(long _maxWaitMillis) {
        startInBackground();
        long sleepMs = 100;
        long waited = 0;

        while (!isRunning()) {
            if (waited >= _maxWaitMillis) {
                throw new RuntimeException("EmbeddedDbusDaemon not started in the specified time of " + _maxWaitMillis + " ms");
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException _ex) {
                LOGGER.debug("Interrupted while waiting for DBus daemon to start");
                break;
            }
            waited += sleepMs;
            LOGGER.debug("Waiting for embedded daemon to start: {} of {} ms waited", waited, _maxWaitMillis);
        }
    }

    /**
     * Whether the DBusDaemon is still running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return daemonThread == null ? false : daemonThread.isRunning();
    }

    /**
     * The currently configured {@link SaslAuthMode}.
     * When null is returned, the {@link SaslAuthMode} of the transport provider is used.
     *
     * @return {@link SaslAuthMode} or null
     */
    public SaslAuthMode getSaslAuthMode() {
        return saslAuthMode;
    }

    /**
     * Use this to override the default authentication mode which would
     * be used by the transport based on the {@link BusAddress}.
     *
     * @param _saslAuthMode auth mode, null to use default
     */
    public void setSaslAuthMode(SaslAuthMode _saslAuthMode) {
        saslAuthMode = _saslAuthMode;
    }

    /**
     * The file owner for the created unix socket.<br>
     * Ignored if TCP is used.<br>
     * <br>
     * Will only work if currently running JVM process user
     * has suitable permissions to change the owner.
     *
     * @param _owner owner to set
     */
    public void setUnixSocketOwner(String _owner) {
        unixSocketFileOwner = _owner;
    }

    /**
     * The file group for the created unix socket.<br>
     * Ignored if TCP is used.<br>
     * <br>
     * Will only work if currently running JVM process user
     * has suitable permissions to change the group.
     *
     * @param _group group to set
     */
    public void setUnixSocketGroup(String _group) {
        unixSocketFileGroup = _group;
    }

    /**
     * The file permissions for the created unix socket.<br>
     * Ignored if TCP is used or if the OS is Windows.<br>
     * <br>
     * Will only work if currently running JVM process user
     * has suitable permissions to change the permissions.
     *
     * @param _permissions permissions to set
     */
    public void setUnixSocketPermissions(PosixFilePermission... _permissions) {
        unixSocketFilePermissions = _permissions;
    }

    /**
     * Start listening for incoming connections.
     * <p>
     * Will throw {@link IllegalArgumentException} if a unsupported transport is used.
     *
     * @throws IOException when connection fails
     * @throws DBusException when the provided bus address is wrong
     */
    private void startListening() throws IOException, DBusException {
        if (!TransportBuilder.getRegisteredBusTypes().contains(address.getBusType().toUpperCase())) {
            throw new IllegalArgumentException("Unknown or unsupported address type: " + address.getType());
        }

        try (AbstractTransport transport = TransportBuilder.create(address)
                .withSaslAuthMode(getSaslAuthMode())
                .withUnixSocketFileOwner(unixSocketFileOwner)
                .withUnixSocketFileGroup(unixSocketFileGroup)
                .withUnixSocketFilePermissions(unixSocketFilePermissions)
                .withAutoConnect(false)
                .build()) {
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
