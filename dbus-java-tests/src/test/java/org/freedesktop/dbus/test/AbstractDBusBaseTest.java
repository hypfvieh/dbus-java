package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.test.helper.SampleClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test providing server and client connection and some default exports.
 */
public abstract class AbstractDBusBaseTest extends AbstractDBusDaemonBaseTest {
    // CHECKSTYLE:OFF
    protected DBusConnection serverconn = null;
    protected DBusConnection clientconn = null;
    protected SampleClass tclass;
    // CHECKSTYLE:ON

    @BeforeEach
    public void setUp() throws DBusException {
        serverconn = DBusConnectionBuilder.forSessionBus().withShared(false).build();
        clientconn = DBusConnectionBuilder.forSessionBus().withShared(false).build();
        serverconn.requestBusName(getTestBusName());

        logger.info("Client-Conn UniqueID: {}, Server-Conn Unique-ID: {}",
            clientconn.getUniqueName(), serverconn.getUniqueName());

        tclass = new SampleClass(serverconn);

        /** This exports an instance of the test class as the object /Test. */
        serverconn.exportObject(getTestObjectPath(), tclass);
        serverconn.addFallback(getTestObjectPath() + "FallbackTest", tclass);
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.debug("Checking for outstanding errors");
        // capture any outstanding errors first, but report them only after both connections were cleaned up,
        // so a pending error can never leave a connection connected / the bus name still owned
        DBusExecutionException serverError = serverconn == null ? null : serverconn.getError();
        DBusExecutionException clientError = clientconn == null ? null : clientconn.getError();

        logger.debug("Disconnecting");
        try {
            if (clientconn != null) {
                clientconn.disconnect();
            }
        } finally {
            if (serverconn != null) {
                try {
                    serverconn.releaseBusName(getTestBusName());
                } finally {
                    serverconn.disconnect();
                }
            }
        }

        if (serverError != null) {
            throw serverError;
        }
        if (clientError != null) {
            throw clientError;
        }
    }

    protected String getTestObjectPath() {
        return "/" + getClass().getSimpleName();
    }

    protected String getTestBusName() {
        return getClass().getName();
    }
}
