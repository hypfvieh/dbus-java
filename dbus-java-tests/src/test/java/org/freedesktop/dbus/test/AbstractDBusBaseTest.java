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
        serverconn = DBusConnectionBuilder.forSessionBus().withShared(false).withWeakReferences(true).build();
        clientconn = DBusConnectionBuilder.forSessionBus().withShared(false).withWeakReferences(true).build();
        serverconn.requestBusName(getTestBusName());

        tclass = new SampleClass(serverconn);

        /** This exports an instance of the test class as the object /Test. */
        serverconn.exportObject(getTestObjectPath(), tclass);
        serverconn.addFallback(getTestObjectPath() + "FallbackTest", tclass);
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.debug("Checking for outstanding errors");
        DBusExecutionException dbee = serverconn.getError();
        if (null != dbee) {
            throw dbee;
        }
        dbee = clientconn.getError();
        if (null != dbee) {
            throw dbee;
        }

        logger.debug("Disconnecting");
        /** Disconnect from the bus. */
        clientconn.disconnect();
        serverconn.releaseBusName(getTestBusName());
        serverconn.disconnect();
    }

    protected String getTestObjectPath() {
        return "/" + getClass().getSimpleName();
    }

    protected String getTestBusName() {
        return getClass().getName();
    }
}
