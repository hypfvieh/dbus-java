/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.errors.ServiceUnknown;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Local;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.test.helper.SampleClass;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.SampleNewInterfaceClass;
import org.freedesktop.dbus.test.helper.SampleSerializable;
import org.freedesktop.dbus.test.helper.callbacks.handler.CallbackHandlerImpl;
import org.freedesktop.dbus.test.helper.interfaces.SampleNewInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.test.helper.signals.SampleSignals;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestArraySignal;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestEmptySignal;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestPathSignal;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestRenamedSignal;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestSignal;
import org.freedesktop.dbus.test.helper.signals.handler.ArraySignalHandler;
import org.freedesktop.dbus.test.helper.signals.handler.BadArraySignalHandler;
import org.freedesktop.dbus.test.helper.signals.handler.DisconnectHandler;
import org.freedesktop.dbus.test.helper.signals.handler.EmptySignalHandler;
import org.freedesktop.dbus.test.helper.signals.handler.ObjectSignalHandler;
import org.freedesktop.dbus.test.helper.signals.handler.PathSignalHandler;
import org.freedesktop.dbus.test.helper.signals.handler.RenamedSignalHandler;
import org.freedesktop.dbus.test.helper.signals.handler.SignalHandler;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.test.helper.structs.SampleStruct2;
import org.freedesktop.dbus.test.helper.structs.SampleStruct3;
import org.freedesktop.dbus.test.helper.structs.SampleTuple;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.hypfvieh.util.TimeMeasure;

/**
 * This is a test program which sends and recieves a signal, implements, exports and calls a remote method.
 */
// CHECKSTYLE:OFF
public class TestAll extends Assert {

    public static final String TEST_OBJECT_PATH = "/TestAll";
    
    // CHECKSTYLE:OFF
    private static DBusConnection serverconn = null;
    private static DBusConnection clientconn = null;
    private static SampleClass tclass;
    // CHECKSTYLE:ON

    @BeforeClass
    public static void beforeClass() throws DBusException {
        serverconn = DBusConnection.getConnection(DBusBusType.SESSION);
        clientconn = DBusConnection.getConnection(DBusBusType.SESSION);
        serverconn.setWeakReferences(true);
        clientconn.setWeakReferences(true);
        serverconn.requestBusName("foo.bar.Test");
        
        tclass = new SampleClass(serverconn);
        
        /** This exports an instance of the test class as the object /Test. */
        serverconn.exportObject(TEST_OBJECT_PATH, tclass);
        serverconn.addFallback("/FallbackTest", tclass);
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("Checking for outstanding errors");
        DBusExecutionException dbee = serverconn.getError();
        if (null != dbee) {
            throw dbee;
        }
        dbee = clientconn.getError();
        if (null != dbee) {
            throw dbee;
        }

        System.out.println("Disconnecting");
        /** Disconnect from the bus. */
        clientconn.disconnect();
        serverconn.disconnect();
    }

    @Test
    public void testSignalHandlers() throws DBusException, InterruptedException {
        SignalHandler sigh = new SignalHandler(1, new UInt32(42), "Bar");
        RenamedSignalHandler rsh = new RenamedSignalHandler(1, new UInt32(42), "Bar");
        EmptySignalHandler esh = new EmptySignalHandler(1);

        ArraySignalHandler ash = new ArraySignalHandler(1);

        /** This gets a remote object matching our bus name and exported object path. */
        Peer peer = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, Peer.class);

        DBus dbus = clientconn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

        System.out.print("Listening for signals...");
        /** This registers an instance of the test class as the signal handler for the TestSignal class. */

        clientconn.addSigHandler(SampleSignals.TestEmptySignal.class, esh);
        clientconn.addSigHandler(SampleSignals.TestSignal.class, sigh);
        clientconn.addSigHandler(SampleSignals.TestRenamedSignal.class, rsh);

        clientconn.addSigHandler(Local.Disconnected.class, new DisconnectHandler(clientconn, rsh));

        String source = dbus.GetNameOwner("foo.bar.Test");

        clientconn.addSigHandler(SampleSignals.TestArraySignal.class, source, peer, ash);
        clientconn.addSigHandler(SampleSignals.TestObjectSignal.class, new ObjectSignalHandler(1));
        clientconn.addSigHandler(SampleSignals.TestPathSignal.class, new PathSignalHandler(1));

        BadArraySignalHandler<TestSignal> bash = new BadArraySignalHandler<TestSignal>(1);
        clientconn.addSigHandler(TestSignal.class, bash);
        clientconn.removeSigHandler(TestSignal.class, bash);
        System.out.println("done");

        System.out.println("Sending Signal");
        /**
         * This creates an instance of the Test Signal, with the given object path, signal name and parameters, and
         * broadcasts in on the Bus.
         */
        serverconn.sendMessage(new TestSignal("/foo/bar/Wibble", "Bar", new UInt32(42)));
        serverconn.sendMessage(new TestEmptySignal("/foo/bar/Wibble"));
        serverconn.sendMessage(new TestRenamedSignal("/foo/bar/Wibble", "Bar", new UInt32(42)));

        // wait some time to receive signals
        Thread.sleep(1000L);

        // ensure callback has been fired at least once
        assertTrue("SignalHandler should have been called", sigh.getActualTestRuns() == 1);
        assertTrue("EmptySignalHandler should have been called", rsh.getActualTestRuns() == 1);

        /** Remove sig handler */
        clientconn.removeSigHandler(SampleSignals.TestSignal.class, sigh);
        clientconn.removeSigHandler(SampleSignals.TestEmptySignal.class, esh);
        clientconn.removeSigHandler(SampleSignals.TestRenamedSignal.class, rsh);

    }

    @Test
    public void testPing() throws DBusException {
        System.out.println("Pinging ourselves");
        Peer peer = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, Peer.class);

        TimeMeasure timeMeasure = new TimeMeasure();
        for (int i = 0; i < 10; i++) {
            timeMeasure.reset();
            peer.Ping();
            System.out.println("Ping returned in " + timeMeasure.getElapsed() + "ms.");
        }

    }

    public void testDbusNames() throws DBusException {
        System.out.println("These things are on the bus:");

        System.out.println("Listening for Method Calls");
        SampleClass tclass = new SampleClass(serverconn);
        SampleClass tclass2 = new SampleClass(serverconn);
        /** This exports an instance of the test class as the object /Test. */
        serverconn.exportObject("/TestClassToFindOnBus", tclass);
        serverconn.exportObject("/SecondTestClassToFindOnBus", tclass2);

        DBus dbus = clientconn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);

        String[] names = dbus.ListNames();
        assertTrue(Arrays.asList(names).contains("/TestClassToFindOnBus"));
        assertTrue(Arrays.asList(names).contains("/SecondTestClassToFindOnBus"));

        serverconn.unExportObject("/SecondTestClassToFindOnBus");
        serverconn.unExportObject("/TestClassToFindOnBus");
    }

    @Test
    public void testIntrospection() throws DBusException {
        System.out.println("Getting our introspection data");
        /** This gets a remote object matching our bus name and exported object path. */
        Introspectable intro = clientconn.getRemoteObject("foo.bar.Test", "/", Introspectable.class);
        intro = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, Introspectable.class);
        /** Get introspection data */
        String data = intro.Introspect();
        assertNotNull(data);
        assertTrue(data.startsWith("<!DOCTYPE"));
    }

    @Test
    public void testCallRemoteMethod() throws DBusException {
        System.out.println("Calling Method0/1");
        /** This gets a remote object matching our bus name and exported object path. */
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);
        System.out.println("Got Remote Object: " + tri);
        /** Call the remote object and get a response. */
        String rname = tri.getName();
        System.out.println("Got Remote Name: " + rname);

        DBusPath path = new DBusPath("/nonexistantwooooooo");
        DBusPath p = tri.pathrv(path);
        System.out.println(path.toString() + " => " + p.toString());
        assertEquals("pathrv incorrect", path, p);

        List<DBusPath> paths = new ArrayList<>();
        paths.add(path);
        
        List<DBusPath> ps = tri.pathlistrv(paths);
        System.out.println(paths.toString() + " => " + ps.toString());

        assertEquals("pathlistrv incorrect", paths, ps);

        Map<DBusPath, DBusPath> pathm = new HashMap<>();
        pathm.put(path, path);
        Map<DBusPath, DBusPath> pm = tri.pathmaprv(pathm);

        System.out.println(pathm.toString() + " => " + pm.toString());
        System.out.println(pm.containsKey(path) + " " + pm.get(path) + " " + path.equals(pm.get(path)));
        System.out.println(pm.containsKey(p) + " " + pm.get(p) + " " + p.equals(pm.get(p)));

        assertTrue("pathmaprv incorrect", pm.containsKey(path));
        assertTrue("pathmaprv incorrect", path.equals(pm.get(path)));
    }

    @Test
    public void testCallGetUtf8String() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);
        /** Call the remote object and get a response. */
        String rname = tri.getName();

        Collator col = Collator.getInstance();
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);

        if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!", rname)) {
            fail("getName return value incorrect");
        }
    }

    @Test
    public void testFloats() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);

        DBusPath path = new DBusPath("/nonexistantwooooooo");
        DBusPath p = tri.pathrv(path);
        System.out.println(path.toString() + " => " + p.toString());
        assertEquals("pathrv incorrect", path, p);

        List<DBusPath> paths = new ArrayList<>();
        paths.add(path);
        List<DBusPath> ps = tri.pathlistrv(paths);
        System.out.println(paths.toString() + " => " + ps.toString());

        Map<DBusPath, DBusPath> pathm = new HashMap<>();
        pathm.put(path, path);

        serverconn.sendMessage(new TestPathSignal(TEST_OBJECT_PATH, path, paths, pathm));

        System.out.println("sending it to sleep");
        tri.waitawhile();
        System.out.println("testing floats");
        if (17.093f != tri.testfloat(new float[] {
                17.093f, -23f, 0.0f, 31.42f
        })) {
            fail("testfloat returned the wrong thing");
        }
    }

    @Test
    public void testStruct() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);

        List<List<Integer>> lli = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        li.add(1);
        li.add(2);
        li.add(3);
        lli.add(li);
        lli.add(li);
        lli.add(li);
        SampleStruct3 ts3 = new SampleStruct3(new SampleStruct2(new ArrayList<>(), new Variant<>(0)), lli);
        int[][] out = tri.teststructstruct(ts3);
        if (out.length != 3) {
            fail("teststructstruct returned the wrong thing: " + Arrays.deepToString(out));
        }
        for (int[] o : out) {
            if (o.length != 3 || o[0] != 1 || o[1] != 2 || o[2] != 3) {
                fail("teststructstruct returned the wrong thing: " + Arrays.deepToString(out));
            }
        }
    }

    public void testFrob() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);
        System.out.println("frobnicating");
        List<Long> ls = new ArrayList<>();
        ls.add(2L);
        ls.add(5L);
        ls.add(71L);
        Map<UInt16, Short> mus = new HashMap<>();
        mus.put(new UInt16(4), (short) 5);
        mus.put(new UInt16(5), (short) 6);
        mus.put(new UInt16(6), (short) 7);
        Map<String, Map<UInt16, Short>> msmus = new HashMap<>();
        msmus.put("stuff", mus);
        int rint = tri.frobnicate(ls, msmus, 13);
        if (-5 != rint) {
            fail("frobnicate return value incorrect");
        }

    }

    @Test
    public void testCallWithCallback() throws DBusException, InterruptedException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);

        System.out.println("Doing stuff asynchronously with callback");
        CallbackHandlerImpl cbWhichWorks = new CallbackHandlerImpl(1, 0);
        clientconn.callWithCallback(tri, "getName", cbWhichWorks);
        System.out.println("Doing stuff asynchronously with callback, which throws an error");
        CallbackHandlerImpl cbWhichThrows = new CallbackHandlerImpl(1, 0);
        clientconn.callWithCallback(tri, "getNameAndThrow", cbWhichThrows);

        /** call something that throws */
        try {
            System.out.println("Throwing stuff");
            tri.throwme();
            TestAll.fail("Method Execution should have failed");
        } catch (SampleException ex) {
            System.out.println("Remote Method Failed with: " + ex.getClass().getName() + " " + ex.getMessage());
            if (!ex.getMessage().equals("test")) {
                TestAll.fail("Error message was not correct");
            }
        }
        
        Thread.sleep(500L); // wait some time to let the callbacks do their work
        
        assertEquals(1, cbWhichWorks.getTestHandleCalls());
        assertEquals(0, cbWhichThrows.getTestHandleCalls());

        assertEquals(0, cbWhichWorks.getTestErrorCalls());
        assertEquals(1, cbWhichThrows.getTestErrorCalls());
    }

    @Test
    public void testException() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);

        /** call something that throws */
        try {
            System.out.println("Throwing stuff");
            tri.throwme();
            fail("Method Execution should have failed");
        } catch (SampleException ex) {
            System.out.println("Remote Method Failed with: " + ex.getClass().getName() + " " + ex.getMessage());
            if (!ex.getMessage().equals("test")) {
                fail("Error message was not correct");
            }
        }

    }

    @Test
    public void testFails() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject("foo.bar.Test", TEST_OBJECT_PATH);

        /** Try and call an invalid remote object */
        try {
            System.out.println("Calling Method2");
            tri = clientconn.getRemoteObject("foo.bar.NotATest", "/Moofle", SampleRemoteInterface.class);
            System.out.println("Got Remote Name: " + tri.getName());
            fail("Method Execution should have failed");
        } catch (ServiceUnknown ex) {
            System.out.println("Remote Method Failed with: " + ex.getClass().getName() + " " + ex.getMessage());
        }

        /** Try and call an invalid remote object */
        try {
            System.out.println("Calling Method3");
            tri = clientconn.getRemoteObject("foo.bar.Test", "/Moofle", SampleRemoteInterface.class);
            System.out.println("Got Remote Name: " + tri.getName());
            fail("Method Execution should have failed");
        } catch (UnknownObject ex) {
            System.out.println("Remote Method Failed with: " + ex.getClass().getName() + " " + ex.getMessage());
        }

        /** Try and call an explicitly unexported object */
        try {
            System.out.println("Calling Method4");
            tri = clientconn.getRemoteObject("foo.bar.Test", "/BadTest", SampleRemoteInterface.class);
            System.out.println("Got Remote Name: " + tri.getName());
            fail("Method Execution should have failed");
        } catch (UnknownObject ex) {
            System.out.println("Remote Method Failed with: " + ex.getClass().getName() + " " + ex.getMessage());
        }
       
    }

     @Test
    public void testFallback() throws DBusException {
        SampleRemoteInterface tri =
                clientconn.getRemoteObject("foo.bar.Test", "/FallbackTest/0/1", SampleRemoteInterface.class);
        Introspectable intro = clientconn.getRemoteObject("foo.bar.Test", "/FallbackTest/0/4", Introspectable.class);

        assertEquals("This Is A UTF-8 Name: س !!", tri.getName());
        assertTrue(intro.Introspect().startsWith("<!DOCTYPE"));
    }
    
    @Test
    public void testGetProperties() throws DBusException {
        Properties prop = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, Properties.class);
        DBusPath prv = (DBusPath) prop.Get("foo.bar", "foo");
        System.out.println("Got path " + prv);

        assertEquals(prv.getPath(), "/nonexistant/path");

    }

    @Test
    public void testExportPath() throws DBusException {
        /** This gets a remote object matching our bus name and exported object path. */
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);
        System.out.print("Calling the other introspect method: ");
        String intro2 = tri2.Introspect();

        Collator col = Collator.getInstance();
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);

        if (0 != col.compare("Not XML", intro2)) {
            fail("Introspect return value incorrect");
        }

    }

    @Test
    public void testResponse() throws DBusException, InterruptedException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);

        System.out.println(tri2.Introspect());
        /** Call the remote object and get a response. */
        SampleTuple<String, List<Integer>, Boolean> rv = tri2.show(234);
        System.out.println("Show returned: " + rv);
        if (!serverconn.getUniqueName().equals(rv.getFirstValue()) || 1 != rv.getSecondValue().size() || 1953 != rv.getSecondValue().get(0)
                || true != rv.getThirdValue().booleanValue()) {
            fail("show return value incorrect (" + rv.getFirstValue() + "," + rv.getSecondValue() + "," + rv.getThirdValue() + ")");
        }

        System.out.println("Doing stuff asynchronously");
        @SuppressWarnings("unchecked")
        DBusAsyncReply<Boolean> stuffreply = (DBusAsyncReply<Boolean>) clientconn.callMethodAsync(tri2, "dostuff",
                new SampleStruct("bar", new UInt32(52), new Variant<>(new Boolean(true))));

        // wait a bit to allow the async call to complete
        Thread.sleep(500L);
        
        assertFalse("bools are broken", tri2.check());

        assertTrue("dostuff return value incorrect", stuffreply.getReply());

    }

    @Test
    public void testArrays() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);

        List<String> l = new ArrayList<>();
        l.add("hi");
        l.add("hello");
        l.add("hej");
        l.add("hey");
        l.add("aloha");
        System.out.println("Sampling Arrays:");
        List<Integer> is = tri2.sampleArray(l, new Integer[] {
                1, 5, 7, 9
        }, new long[] {
                2, 6, 8, 12
        });
        System.out.println("sampleArray returned an array:");
        for (Integer i : is) {
            System.out.println("--" + i);
        }
        
        assertEquals(5, is.size());
        assertEquals(-1, is.get(0).intValue());
        assertEquals(-5, is.get(1).intValue());
        assertEquals(-7, is.get(2).intValue());
        assertEquals(-12, is.get(3).intValue());
        assertEquals(-18, is.get(4).intValue());

        assertEquals("Didn't get the correct this", tclass, tri2.getThis(tri2));
        
        System.out.print("Sending Array Signal...");
        /**
         * This creates an instance of the Test Signal, with the given object path, signal name and parameters, and
         * broadcasts in on the Bus.
         */
        List<SampleStruct2> tsl = new ArrayList<>();
        tsl.add(new SampleStruct2(l, new Variant<>(new UInt64(567))));
        Map<UInt32, SampleStruct2> tsm = new HashMap<>();
        tsm.put(new UInt32(1), new SampleStruct2(l, new Variant<>(new UInt64(678))));
        tsm.put(new UInt32(42), new SampleStruct2(l, new Variant<>(new UInt64(789))));
        serverconn.sendMessage(new TestArraySignal(TEST_OBJECT_PATH, tsl, tsm));

    }

    public void testSerialization() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);
        List<Integer> v = new ArrayList<>();
        v.add(1);
        v.add(2);
        v.add(3);
        SampleSerializable<String> s = new SampleSerializable<>(1, "woo", v);
        s = tri2.testSerializable((byte) 12, s, 13);
        System.out.print("returned: " + s);
        if (s.getInt() != 1 || !s.getString().equals("woo") || s.getList().size() != 3 || s.getList().get(0) != 1
                || s.getList().get(1) != 2 || s.getList().get(2) != 3) {
            fail("Didn't get back the same TestSerializable");
        }
    }

    @Test
    public void testComplex() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);
        Collator col = Collator.getInstance();
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);

        Map<String, String> m = new HashMap<>();
        m.put("cow", "moo");
        tri2.complexv(new Variant<>(m, "a{ss}"));
        System.out.println("done");

        System.out.print("testing recursion...");

        if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!", tri2.recursionTest())) {
            fail("recursion test failed");
        }

        System.out.println("done");

        System.out.print("testing method overloading...");
        SampleRemoteInterface tri = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface.class);
        if (1 != tri2.overload("foo")) {
            TestAll.fail("wrong overloaded method called");
        }
        if (2 != tri2.overload((byte) 0)) {
            TestAll.fail("wrong overloaded method called");
        }
        if (3 != tri2.overload()) {
            TestAll.fail("wrong overloaded method called");
        }
        if (4 != tri.overload()) {
            TestAll.fail("wrong overloaded method called");
        }
    }

    @Test
    public void testOverload() throws DBusException {
        System.out.print("testing method overloading...");
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);
        SampleRemoteInterface tri = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface.class);

        assertEquals(1, tri2.overload("foo"));
        assertEquals(2, tri2.overload((byte) 0));
        assertEquals(3, tri2.overload());
        assertEquals(4, tri.overload());
    }

    @Test
    public void testRegression13291() throws DBusException {
        SampleRemoteInterface tri = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface.class);

        System.out.print("reg13291...");
        byte[] as = new byte[10];
        for (int i = 0; i < 10; i++) {
            as[i] = (byte) (100 - i);
        }
        
        tri.reg13291(as, as);
        System.out.println("done");
    }

    @Test
    public void testNestedLists() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);
        List<List<Integer>> lli = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        li.add(1);
        lli.add(li);

        List<List<Integer>> reti = tri2.checklist(lli);
        if (reti.size() != 1 || reti.get(0).size() != 1 || reti.get(0).get(0) != 1) {
            TestAll.fail("Failed to check nested lists");
        }
    }

    @Test
    public void testDynamicObjectCreation() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject("foo.bar.Test", TEST_OBJECT_PATH, SampleRemoteInterface2.class);

        SampleNewInterface tni = tri2.getNew();

        assertEquals(tni.getName(), SampleNewInterfaceClass.class.getSimpleName());
    }

}
