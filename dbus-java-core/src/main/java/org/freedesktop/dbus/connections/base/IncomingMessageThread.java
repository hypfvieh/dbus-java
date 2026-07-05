package org.freedesktop.dbus.connections.base;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

public class IncomingMessageThread extends Thread {
    private final Logger             logger = LoggerFactory.getLogger(getClass());

    private volatile boolean         terminate;
    private final ConnectionMessageHandler connection;

    public IncomingMessageThread(ConnectionMessageHandler _connection, BusAddress _busAddress) {
        connection = Objects.requireNonNull(_connection);
        setName("DBusConnection [listener=" + _busAddress.isListeningSocket() + "]");
        setDaemon(true);
    }

    public void terminate() {
        terminate = true;
        interrupt();
    }

    @Override
    public void run() {

        while (!terminate) {
            // read from the wire
            try {
                // this blocks on outgoing being non-empty or a message being available.
                Message msg = connection.readIncoming();
                if (msg != null) {
                    logger.trace("Read message from {}: {}", connection.getTransport(), msg);

                    connection.handleMessage(msg);
                }
            } catch (DBusException | RuntimeException _ex) {
                if (terminate) { // requested termination, ignore failures
                    return;
                }

                if (_ex instanceof FatalException) {
                    logger.error("FatalException in connection thread", _ex);
                    disconnect(_ex);
                    return;
                }

                if (_ex instanceof RuntimeException) {
                    // unexpected runtime failure (e.g. a malformed message): do not keep spinning the
                    // read loop, disconnect instead to avoid a busy-loop and follow the D-Bus spec
                    logger.error("Unexpected runtime exception in connection thread, disconnecting", _ex);
                    disconnect(_ex);
                    return;
                }

                // non-fatal DBusException: log and keep processing further messages
                logger.error("Exception in connection thread", _ex);
            }
        }
    }

    private void disconnect(Throwable _ex) {
        if (connection.isConnected()) {
            terminate = true;
            if (_ex.getCause() instanceof IOException ioe) {
                connection.internalDisconnect(ioe);
            } else {
                connection.internalDisconnect(null);
            }
        }
    }
}
