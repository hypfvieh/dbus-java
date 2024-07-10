package org.freedesktop.dbus.test;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.util.stream.Stream;

@EnabledIf(value = "isFileDescriptorSupported", disabledReason = "file descriptors not supported with the current transport")
public class FileDescriptorsTest extends AbstractDBusDaemonBaseTest {
    public static final String TEST_OBJECT_PATH = "/FileDescriptorsTest";

    private DBusConnection serverconn = null;
    private DBusConnection clientconn = null;

    @BeforeEach
    public void setUp() throws DBusException {
        serverconn = DBusConnectionBuilder.forSessionBus().withShared(false).build();
        clientconn = DBusConnectionBuilder.forSessionBus().withShared(false).build();
        serverconn.requestBusName("foo.bar.Test");
        serverconn.exportObject(TEST_OBJECT_PATH, new FDPassingImpl());
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
        /* Disconnect from the bus. */
        clientconn.disconnect();
        serverconn.releaseBusName("foo.bar.Test");
        serverconn.disconnect();
    }

    public static boolean isFileDescriptorSupported() throws DBusException, IOException {
        if (!TransportBuilder.getRegisteredBusTypes().contains("UNIX")) {
            return false;
        }
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            return conn.isFileDescriptorSupported();
        }
    }

    @Test
    public void fileDescriptorPassing() throws DBusException {
        FDPassing remoteObject = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, FDPassing.class);
        Stream.of(0, 1, 2).map(FileDescriptor::new).forEach(fd -> {
            // that's not a mistake of using NotEquals here, as fd passing make a new copy with a new value
            assertNotEquals(fd.getIntFileDescriptor(), remoteObject.doNothing(fd).getIntFileDescriptor());
        });
    }

    public interface FDPassing extends DBusInterface {
        FileDescriptor doNothing(FileDescriptor _fd);
    }

    private static final class FDPassingImpl implements FDPassing {

        @Override
        public String getObjectPath() {
            return TEST_OBJECT_PATH;
        }

        @Override
        public FileDescriptor doNothing(FileDescriptor _fd) {
            return _fd;
        }
    }
}
