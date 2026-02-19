package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.callbacks.handler.CallbackHandlerImpl;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodTest extends AbstractDBusBaseTest {
    @Test
    public void testCallRemoteMethod() throws DBusException {
        logger.debug("Calling Method0/1");
        /** This gets a remote object matching our bus name and exported object path. */
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());
        logger.debug("Got Remote Object: " + tri);
        /** Call the remote object and get a response. */
        String rname = tri.getName();
        logger.debug("Got Remote Name: " + rname);

        List<Type> ts = new ArrayList<>();
        Marshalling.getJavaType("ya{si}", ts, -1);
        tri.sig(ts.toArray(new Type[0]));

        DBusPath path = new DBusPath("/nonexistantwooooooo");
        DBusPath p = tri.pathrv(path);
        logger.debug("{} => {}", path, p.toString());
        assertEquals(path, p, "pathrv incorrect");

        List<DBusPath> paths = new ArrayList<>();
        paths.add(path);

        List<DBusPath> ps = tri.pathlistrv(paths);
        logger.debug("{} => {}", paths, ps.toString());

        assertEquals(paths, ps, "pathlistrv incorrect");

        Map<DBusPath, DBusPath> pathm = new HashMap<>();
        pathm.put(path, path);
        Map<DBusPath, DBusPath> pm = tri.pathmaprv(pathm);

        assertNotEquals(pathm.getClass(), pm.getClass(), "Expected different map implementations");

        logger.debug("{} => {}", pathm, pm);
        logger.debug("{} {} {}", pm.containsKey(path), pm.get(path), path.equals(pm.get(path)));
        logger.debug("{} {} {}", pm.containsKey(p), pm.get(p), p.equals(pm.get(p)));

        System.out.println("path hashcode: " + path.hashCode());
        System.out.println("pm key hashcode: " + pm.keySet().iterator().next().hashCode());

        assertTrue(pm.containsKey(path), "pathmaprv missing in map");
        assertEquals(path, pm.get(path), "pathmaprv incorrect value");
    }

    @Test
    public void testCallWithCallback() throws DBusException, InterruptedException {
        final SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getRemoteObject(getTestBusName(), getTestObjectPath());

        logger.debug("Doing stuff asynchronously with callback");
        CallbackHandlerImpl cbWhichWorks = new CallbackHandlerImpl();
        clientconn.callWithCallback(tri, "getName", cbWhichWorks);

        logger.debug("Doing stuff asynchronously with callback, which throws an error");
        CallbackHandlerImpl cbWhichThrows = new CallbackHandlerImpl();
        clientconn.callWithCallback(tri, "getNameAndThrow", cbWhichThrows);

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

        Thread.sleep(500L); // wait some time to let the callbacks do their work

        // we do not expect any test failures
        assertNull(cbWhichThrows.getLastError());
        assertNull(cbWhichWorks.getLastError());

        assertEquals(1, cbWhichWorks.getTestHandleCalls());
        assertEquals(0, cbWhichThrows.getTestHandleCalls());

        assertEquals(0, cbWhichWorks.getTestErrorCalls());
        assertEquals(1, cbWhichThrows.getTestErrorCalls());
    }
}
