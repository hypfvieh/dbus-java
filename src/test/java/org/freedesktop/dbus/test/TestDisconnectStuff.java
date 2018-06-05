package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.test.helper.SampleClass;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.junit.jupiter.api.Test;

public class TestDisconnectStuff {

    @Test
    public void testStuffAfterDisconnect() throws DBusException, InterruptedException {

        DBusConnection serverConnection = DBusConnection.getConnection(DBusBusType.SESSION);
        DBusConnection clientConnection = DBusConnection.getConnection(DBusBusType.SESSION);
        serverConnection.setWeakReferences(true);
        clientConnection.setWeakReferences(true);

        serverConnection.requestBusName("foo.bar.why.again.Test");

        SampleClass tclass = new SampleClass(serverConnection);

        serverConnection.exportObject("/Test2000", tclass);

        SampleRemoteInterface tri =
                clientConnection.getRemoteObject("foo.bar.why.again.Test", "/Test2000", SampleRemoteInterface.class);


        /** Call a method when disconnected */
        try {
            clientConnection.disconnect();
            serverConnection.disconnect();

            System.out.println("getName() suceeded and returned: " + tri.getName());
            fail("Should not succeed when disconnected");
        } catch (NotConnected exnc) {
        }
    }
}
