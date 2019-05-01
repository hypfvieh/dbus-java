package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MarshallingFileDescriptorTest {

    private static final String TEST_OBJECT_PATH = "/TestFileDescriptor";
    private static final String TEST_BUSNAME = "foo.bar.TestFileDescriptor";
    
    private DBusConnection serverConn;
    private DBusConnection clientConn;
    
    private FileInputStream sampleFileStream;
    
    @BeforeEach
    public void before() throws DBusException, FileNotFoundException, IOException {
        serverConn = DBusConnection.getConnection(DBusBusType.SESSION);
        clientConn = DBusConnection.getConnection(DBusBusType.SESSION);
        serverConn.setWeakReferences(true);
        clientConn.setWeakReferences(true);
        serverConn.requestBusName(TEST_BUSNAME);

        sampleFileStream = new FileInputStream(File.createTempFile("dbustest", "testFd"));
        
        GetFileDescriptor fd = new GetFileDescriptor(sampleFileStream.getFD());
         
        System.out.println("Created file descriptor: " + getFileDescriptorIntId(sampleFileStream.getFD()));
        
        serverConn.exportObject(TEST_OBJECT_PATH, fd);
    }

    @AfterEach
    public void after() throws IOException {
        DBusExecutionException dbee = serverConn.getError();
        if (null != dbee) {
            throw dbee;
        }
        dbee = clientConn.getError();
        if (null != dbee) {
            throw dbee;
        }
        
        clientConn.disconnect();
        serverConn.disconnect();
        sampleFileStream.close();
    }
    
    @Test
    public void testFileDescriptor() throws DBusException, IOException {
        DBusInterface remoteObject = clientConn.getRemoteObject("foo.bar.TestFileDescriptor", TEST_OBJECT_PATH, IFileDescriptor.class);

        assertTrue(remoteObject instanceof IFileDescriptor, "Expected instance of GetFileDescriptor");
        
        FileDescriptor fileDescriptor = ((IFileDescriptor) remoteObject).getFileDescriptor();
        assertNotNull(fileDescriptor, "Descriptor should not be null");
        
        assertTrue(fileDescriptor.valid(), "Descriptor has to be valid");
        int receivedFdId = getFileDescriptorIntId(fileDescriptor);
        System.out.println("Received file descriptor with ID: " + receivedFdId);
        assertEquals(getFileDescriptorIntId(sampleFileStream.getFD()), receivedFdId);
    }
    
    static int getFileDescriptorIntId(FileDescriptor _fd) {
        try {
            Field field = _fd.getClass().getDeclaredField("fd");
            field.setAccessible(true);
            return field.getInt(_fd);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException _ex) {
            return -99;
        }
    }
    
    // ==================================================================================================

    public static class GetFileDescriptor implements IFileDescriptor {

        private final FileDescriptor fileDescriptor;

        public GetFileDescriptor(FileDescriptor _descriptor) {
            fileDescriptor = _descriptor;
        }
        
        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public FileDescriptor getFileDescriptor() {
            return fileDescriptor;
        }
        
    }
    
    @DBusInterfaceName("foo.bar.TestFileDescriptor")
    public static interface IFileDescriptor extends DBusInterface {

        FileDescriptor getFileDescriptor();
    }
}
