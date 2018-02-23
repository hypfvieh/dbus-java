package org.freedesktop.dbus.connections;

import java.util.Objects;

import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionThread extends Thread {
    private final Logger             logger = LoggerFactory.getLogger(getClass());

    private boolean                  terminate;
    private final AbstractConnection connection;

    public ConnectionThread(AbstractConnection _connection) {
        Objects.requireNonNull(_connection);
        connection = _connection;
        setName("DBusConnection");
    }

    public void setTerminate(boolean _terminate) {
        terminate = _terminate;
        interrupt();
    }

    @Override
    public void run() {

        Message m = null;
        while (!terminate) {
            m = null;

            // read from the wire
            try {
                // this blocks on outgoing being non-empty or a message being available.
                m = connection.readIncoming();
                if (m != null) {
                    logger.trace("Got Incoming Message: " + m);

                    connection.handleMessage(m);

                    m = null;
                }
            } catch (DBusException e) {
                logger.error("Exception in connection thread.", e);
                if (e instanceof FatalException) {
                    connection.disconnect();
                }
            }
        }
    }
}
