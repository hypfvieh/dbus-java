package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.errors.ServiceUnknown;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.SampleSerializable;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.utils.TimeMeasure;
import org.junit.jupiter.api.Test;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseFunctionsTest extends AbstractDBusBaseTest {

    @Test
    public void testPing() throws DBusException {
        logger.debug("Pinging ourselves");
        Peer peer = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), Peer.class);

        TimeMeasure timeMeasure = new TimeMeasure();
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                timeMeasure.reset();
                peer.Ping();
                logger.debug("Ping returned in " + timeMeasure.getElapsed() + "ms.");
            }
        });

    }

    @Test
    public void testDbusNames() throws DBusException, InterruptedException {
        DBus dbus = clientconn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

        String[] names = dbus.ListNames();
        logger.debug("Names on bus: {}", Arrays.toString(names));
        assertTrue(Arrays.asList(names).contains(getTestBusName()));
    }

    @Test
    public void testSerialization() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        List<Integer> v = new ArrayList<>();
        v.add(1);
        v.add(2);
        v.add(3);
        SampleSerializable<String> s = new SampleSerializable<>(1, "woo", v);
        s = tri2.testSerializable((byte) 12, s, 13);
        logger.debug("returned: " + s);
        if (s.getInt() != 1 || !s.getString().equals("woo") || s.getList().size() != 3 || s.getList().get(0) != 1
                || s.getList().get(1) != 2 || s.getList().get(2) != 3) {
            fail("Didn't get back the same TestSerializable");
        }
    }

    @Test
    public void testIntrospection() throws DBusException {
        logger.debug("Getting our introspection data");
        /** This gets a remote object matching our bus name and exported object path. */
        Introspectable intro = clientconn.getRemoteObject(getTestBusName(), "/", Introspectable.class);
        intro = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), Introspectable.class);
        /** Get introspection data */
        String data = intro.Introspect();
        assertNotNull(data);
        assertTrue(data.startsWith("<!DOCTYPE"));
    }

    @Test
    public void testExportPath() throws DBusException {
        /** This gets a remote object matching our bus name and exported object path. */
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        logger.debug("Calling the other introspect method: ");
        String intro2 = tri2.Introspect();

        Collator col = Collator.getInstance();
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);

        if (0 != col.compare("Not XML", intro2)) {
            fail("Introspect return value incorrect");
        }

    }

    @Test
    public void testGetProperties() throws DBusException {
        Properties prop = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), Properties.class);
        DBusPath prv = (DBusPath) prop.Get("foo.bar", "foo");
        logger.debug("Got path " + prv);

        assertEquals("/nonexistant/path", prv.getPath());

    }

    @Test
    public void testException() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

        /** call something that throws */
        try {
            logger.debug("Throwing stuff");
            tri.throwme();
            fail("Method Execution should have failed");
        } catch (SampleException _ex) {
            logger.debug("Remote Method Failed with: " + _ex.getClass().getName() + " " + _ex.getMessage());
            if (!_ex.getMessage().equals("test")) {
                fail("Error message was not correct");
            }
        }

    }

    @Test
    public void testFails() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

        /** Try and call an invalid remote object */
        try {
            logger.debug("Calling Method2");
            tri = clientconn.getRemoteObject("foo.bar.NotATest", "/Moofle", SampleRemoteInterface.class);
            logger.debug("Got Remote Name: " + tri.getName());
            fail("Method Execution should have failed");
        } catch (ServiceUnknown _ex) {
            logger.debug("Remote Method Failed with: " + _ex.getClass().getName() + " " + _ex.getMessage());
        }

        /** Try and call an invalid remote object */
        try {
            logger.debug("Calling Method3");
            tri = clientconn.getRemoteObject(getTestBusName(), "/Moofle", SampleRemoteInterface.class);
            logger.debug("Got Remote Name: " + tri.getName());
            fail("Method Execution should have failed");
        } catch (UnknownObject _ex) {
            logger.debug("Remote Method Failed with: " + _ex.getClass().getName() + " " + _ex.getMessage());
        }

        /** Try and call an explicitly unexported object */
        try {
            logger.debug("Calling Method4");
            tri = clientconn.getRemoteObject(getTestBusName(), "/BadTest", SampleRemoteInterface.class);
            logger.debug("Got Remote Name: " + tri.getName());
            fail("Method Execution should have failed");
        } catch (UnknownObject _ex) {
            logger.debug("Remote Method Failed with: " + _ex.getClass().getName() + " " + _ex.getMessage());
        }

    }

    @Test
    public void testOverload() throws DBusException {
        logger.debug("testing method overloading...");
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        SampleRemoteInterface tri = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface.class);

        assertEquals(1, tri2.overload("foo"));
        assertEquals(2, tri2.overload((byte) 0));
        assertEquals(3, tri2.overload());
        assertEquals(4, tri.overload());
    }
}
