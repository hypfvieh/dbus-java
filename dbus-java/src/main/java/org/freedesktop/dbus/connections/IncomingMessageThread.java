package org.freedesktop.dbus.connections;

import java.util.Objects;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingMessageThread extends Thread {
    private final Logger             logger = LoggerFactory.getLogger(getClass());

    private boolean                  terminate;
    private final AbstractConnection connection;

    public IncomingMessageThread(AbstractConnection _connection) {
        Objects.requireNonNull(_connection);
        connection = _connection;
        setName("DBusConnection");
        setDaemon(true);
    }

    public void setTerminate(boolean _terminate) {
        terminate = _terminate;
        interrupt();
    }

    @Override
    public void run() {

        Message msg = null;
        while (!terminate) {
            msg = null;

            // read from the wire
            try {
                // this blocks on outgoing being non-empty or a message being available.
                msg = connection.readIncoming();
                if (msg != null) {
                    logger.trace("Got Incoming Message: {}", msg);

                    connection.handleMessage(msg);

                    msg = null;
                }
            } catch (DBusException _ex) {
                if (_ex instanceof FatalException) {
                    logger.error("FatalException in connection thread.", _ex);
                    if (connection.isConnected()) {
                        connection.disconnect();
                        setTerminate(true);
                    }
                }

                if (!terminate) { // only log exceptions if the connection was not intended to be closed
                    logger.error("Exception in connection thread.", _ex);
                }
            }
        }
    }
}
