package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterfaceEnum.TestEnum;
import org.freedesktop.dbus.test.helper.signals.SampleSignals;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.*;
import org.freedesktop.dbus.test.helper.signals.handler.*;
import org.freedesktop.dbus.test.helper.structs.SampleStruct2;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.util.*;

public class HandlerTest extends AbstractDBusBaseTest {
    @Test
    public void testSignalHandlers() throws DBusException, InterruptedException {
        SignalHandler sigh = new SignalHandler(1, new UInt32(42), "Bar");
        RenamedSignalHandler rsh = new RenamedSignalHandler(1, new UInt32(42), "Bar");
        EmptySignalHandler esh = new EmptySignalHandler(1);
        ArraySignalHandler ash = new ArraySignalHandler(1);
        final ObjectSignalHandler osh = new ObjectSignalHandler(1);
        final PathSignalHandler psh = new PathSignalHandler(1);
        final EnumSignalHandler ensh = new EnumSignalHandler(1);

        /** This gets a remote object matching our bus name and exported object path. */
        SampleRemoteInterface peer = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

        DBus dbus = clientconn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

        logger.debug("Listening for signals...");
        /** This registers an instance of the test class as the signal handler for the TestSignal class. */

        clientconn.addSigHandler(SampleSignals.TestEmptySignal.class, esh);
        clientconn.addSigHandler(SampleSignals.TestSignal.class, sigh);
        clientconn.addSigHandler(SampleSignals.TestRenamedSignal.class, rsh);

        String source = dbus.GetNameOwner(getTestBusName());

        clientconn.addSigHandler(SampleSignals.TestArraySignal.class, source, peer, ash);
        clientconn.addSigHandler(SampleSignals.TestObjectSignal.class, osh);
        clientconn.addSigHandler(SampleSignals.TestPathSignal.class, psh);
        clientconn.addSigHandler(SampleSignals.TestEnumSignal.class, ensh);

        BadArraySignalHandler<TestSignal> bash = new BadArraySignalHandler<>(1);
        clientconn.addSigHandler(TestSignal.class, bash);
        clientconn.removeSigHandler(TestSignal.class, bash);
        logger.debug("done");

        logger.debug("Sending Signals");

        /**
         * This creates an instance of the Test Signal, with the given object path, signal name and parameters, and
         * broadcasts in on the Bus.
         */
        serverconn.sendMessage(new TestSignal("/foo/bar/Wibble", "Bar", new UInt32(42)));
        serverconn.sendMessage(new TestEmptySignal("/foo/bar/Wibble"));
        serverconn.sendMessage(new TestRenamedSignal("/foo/bar/Wibble", "Bar", new UInt32(42)));

        logger.debug("Sending Path Signal...");
        DBusPath path = new DBusPath("/nonexistantwooooooo");
        DBusPath p = peer.pathrv(path);
        logger.debug(path.toString() + " => " + p.toString());
        assertEquals(path, p, "pathrv incorrect");
        List<DBusPath> paths = new ArrayList<>();
        paths.add(path);
        List<DBusPath> ps = peer.pathlistrv(paths);
        logger.debug(paths.toString() + " => " + ps.toString());
        Map<DBusPath, DBusPath> pathm = new HashMap<>();
        pathm.put(path, path);
        serverconn.sendMessage(new TestPathSignal(getTestObjectPath(), path, paths, pathm));

        logger.debug("Sending Array Signal...");
        final List<SampleStruct2> tsl = new ArrayList<>();
        List<String> l = new ArrayList<>();
        l.add("hi");
        l.add("hello");
        l.add("hej");
        l.add("hey");
        l.add("aloha");
        tsl.add(new SampleStruct2(l, new Variant<>(new UInt64(567))));
        Map<UInt32, SampleStruct2> tsm = new HashMap<>();
        tsm.put(new UInt32(1), new SampleStruct2(l, new Variant<>(new UInt64(678))));
        tsm.put(new UInt32(42), new SampleStruct2(l, new Variant<>(new UInt64(789))));
        serverconn.sendMessage(new TestArraySignal(getTestObjectPath(), tsl, tsm));

        logger.debug("Sending Object Signal...");
        serverconn.sendMessage(new TestObjectSignal(getTestObjectPath(), tclass));

        logger.debug("Sending Enum Signal...");
        serverconn.sendMessage(new TestEnumSignal(getTestObjectPath(), TestEnum.TESTVAL1, Arrays.asList(TestEnum.TESTVAL2, TestEnum.TESTVAL3)));

        // wait some time to receive signals
        Thread.sleep(1000L);

        // ensure callback has been fired at least once
        assertEquals(1, sigh.getActualTestRuns(), "SignalHandler should have been called");
        assertEquals(1, esh.getActualTestRuns(), "EmptySignalHandler should have been called");
        assertEquals(1, rsh.getActualTestRuns(), "RenamedSignalHandler should have been called");
        assertEquals(1, ash.getActualTestRuns(), "ArraySignalHandler should have been called");
        assertEquals(1, ensh.getActualTestRuns(), "EnumSignalHandler should have been called");
        assertEquals(1, psh.getActualTestRuns(), "PathSignalHandler should have been called");
        assertEquals(1, osh.getActualTestRuns(), "ObjectSignalHandler should have been called");

        assertDoesNotThrow(sigh::throwAssertionError);
        assertDoesNotThrow(esh::throwAssertionError);
        assertDoesNotThrow(rsh::throwAssertionError);
        assertDoesNotThrow(ash::throwAssertionError);
        assertDoesNotThrow(ensh::throwAssertionError);
        assertDoesNotThrow(psh::throwAssertionError);
        assertDoesNotThrow(osh::throwAssertionError);

        /** Remove sig handler */
        clientconn.removeSigHandler(SampleSignals.TestSignal.class, sigh);
        clientconn.removeSigHandler(SampleSignals.TestEmptySignal.class, esh);
        clientconn.removeSigHandler(SampleSignals.TestRenamedSignal.class, rsh);
        clientconn.removeSigHandler(SampleSignals.TestArraySignal.class, ash);
        clientconn.removeSigHandler(SampleSignals.TestObjectSignal.class, osh);
        clientconn.removeSigHandler(SampleSignals.TestPathSignal.class, psh);
        clientconn.removeSigHandler(SampleSignals.TestEnumSignal.class, ensh);

    }

    @Test
    public void testGenericSignalHandler() throws DBusException, InterruptedException {
        GenericSignalHandler genericHandler = new GenericSignalHandler();
        DBusMatchRule signalRule = new DBusMatchRule("signal", "org.foo", "methodnoarg", "/");

        clientconn.addGenericSigHandler(signalRule, genericHandler);

        DBusSignal signalToSend = clientconn.getMessageFactory().createSignal(null, "/", "org.foo", "methodnoarg", null);

        serverconn.sendMessage(signalToSend);

        // wait some time to receive signals
        Thread.sleep(1000L);

        // ensure callback has been fired at least once
        assertEquals(1, genericHandler.getActualTestRuns(), "GenericHandler should have been called");

        clientconn.removeGenericSigHandler(signalRule, genericHandler);
    }

    @Test
    public void testGenericDecodeSignalHandler() throws DBusException, InterruptedException {
        GenericHandlerWithDecode genericDecode = new GenericHandlerWithDecode(new UInt32(42), "SampleString");
        DBusMatchRule signalRule = new DBusMatchRule("signal", "org.foo", "methodarg", "/");

        clientconn.addGenericSigHandler(signalRule, genericDecode);

        DBusSignal signalToSend = serverconn.getMessageFactory().createSignal(null, "/", "org.foo", "methodarg", "us", new UInt32(42), "SampleString");

        serverconn.sendMessage(signalToSend);

        // wait some time to receive signals
        Thread.sleep(1000L);

        assertDoesNotThrow(() -> {
            genericDecode.incomingSameAsExpected();
            clientconn.removeGenericSigHandler(signalRule, genericDecode);
        });

        assertNull(genericDecode.getAssertionError());
    }

    @Test
    public void testGenericHandlerWithNoInterface() throws DBusException, InterruptedException {
        GenericHandlerWithDecode genericDecode = new GenericHandlerWithDecode(new UInt32(42), "SampleString");
        DBusMatchRule signalRule = new DBusMatchRule("signal", null, "methodargNoIface", "/");

        clientconn.addGenericSigHandler(signalRule, genericDecode);
        DBusSignal signalToSend = clientconn.getMessageFactory().createSignal(null, "/", "org.foo", "methodargNoIface", "us", new UInt32(42), "SampleString");

        serverconn.sendMessage(signalToSend);

        // wait some time to receive signals
        Thread.sleep(1000L);

        assertDoesNotThrow(() -> {
            genericDecode.incomingSameAsExpected();
            clientconn.removeGenericSigHandler(signalRule, genericDecode);
        });

        assertNull(genericDecode.getAssertionError());
    }

    @Test
   public void testFallbackHandler() throws DBusException {
       SampleRemoteInterface tri =
               clientconn.getRemoteObject(getTestBusName(), getTestObjectPath() + "FallbackTest/0/1", SampleRemoteInterface.class);
       Introspectable intro = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath() + "FallbackTest/0/4", Introspectable.class);

       assertEquals("This Is A UTF-8 Name: س !!", tri.getName());
       assertTrue(intro.Introspect().startsWith("<!DOCTYPE"));
   }
}
