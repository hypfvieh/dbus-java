package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.SampleClass;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestShared extends AbstractDBusDaemonBaseTest {

    public static final String TEST_OBJECT_PATH = "/TestAll";

    // CHECKSTYLE:OFF
    private DBusConnection serverconn = null;
    private DBusConnection clientconn = null;
    private SampleClass tclass;
    // CHECKSTYLE:ON

    @BeforeEach
    public void setUp() throws DBusException {
        serverconn = DBusConnectionBuilder.forSessionBus().withShared(true).withExportWeakReferences(true).build();
        clientconn = DBusConnectionBuilder.forSessionBus().withShared(true).withExportWeakReferences(true).build();
        serverconn.requestBusName("foo.bar.Test");

        tclass = new SampleClass(serverconn);

        /** This exports an instance of the test class as the object /Test. */
        serverconn.exportObject(TEST_OBJECT_PATH, tclass);
        serverconn.addFallback("/FallbackTest", tclass);
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
        serverconn.releaseBusName("foo.bar.Test");
        serverconn.disconnect();
    }

    /**
     * This test case is only valid in on shared connections.
     * This is because when two distinct connections are used,
     * the instanced returned by 'getThis' is always a RemoteInvocationProxy
     * which will never be equal to the original object.
     *
     * @throws DBusException on connection failure
     */
    @Test
    public void testGetThis() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);

        DBusInterface other = tri2.getThis(tri2);
        assertEquals(tclass, other, "Didn't get the correct this");
    }

}
