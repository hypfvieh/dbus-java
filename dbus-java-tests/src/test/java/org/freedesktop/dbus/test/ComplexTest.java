package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.helper.SampleNewInterfaceClass;
import org.freedesktop.dbus.test.helper.interfaces.SampleNewInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.test.helper.structs.SampleTuple;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexTest extends AbstractDBusBaseTest {

    @Test
    public void testDbusIgnore() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());
        assertThrowsExactly(UnknownMethod.class, () -> {
            tri.thisShouldBeIgnored();
        });
    }

    public void testFrob() throws DBusException {
        final SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());
        logger.debug("frobnicating");
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
    public void testResponse() throws DBusException, InterruptedException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);

        logger.debug(tri2.Introspect());
        /** Call the remote object and get a response. */
        SampleTuple<String, List<Integer>, Boolean> rv = tri2.show(234);
        logger.debug("Show returned: " + rv);
        if (!clientconn.getUniqueName().equals(rv.getFirstValue()) || 1 != rv.getSecondValue().size() || 1953 != rv.getSecondValue().get(0)
                || !rv.getThirdValue().booleanValue()) {
            fail("show return value incorrect (" + rv.getFirstValue() + "," + rv.getSecondValue() + "," + rv.getThirdValue() + ")");
        }

        logger.debug("Doing stuff asynchronously");
        @SuppressWarnings("unchecked")
        DBusAsyncReply<Boolean> stuffreply = (DBusAsyncReply<Boolean>) clientconn.callMethodAsync(tri2, "dostuff",
                new SampleStruct("bar", new UInt32(52), new Variant<>(Boolean.TRUE)));

        // wait a bit to allow the async call to complete
        Thread.sleep(500L);

        assertFalse(tri2.check(), "bools are broken");

        assertTrue(stuffreply.getReply(), "dostuff return value incorrect");

    }

    @Test
    public void testComplex() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        Collator col = Collator.getInstance();
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);

        Map<String, String> m = new HashMap<>();
        m.put("cow", "moo");
        tri2.complexv(new Variant<>(m, "a{ss}"));
        logger.debug("done");

        logger.debug("testing recursion...");

        if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!", tri2.recursionTest(getTestBusName(), getTestObjectPath()))) {
            fail("recursion test failed");
        }

        logger.debug("done");

        logger.debug("testing method overloading...");
        SampleRemoteInterface tri = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface.class);
        if (1 != tri2.overload("foo")) {
            fail("wrong overloaded method called");
        }
        if (2 != tri2.overload((byte) 0)) {
            fail("wrong overloaded method called");
        }
        if (3 != tri2.overload()) {
            fail("wrong overloaded method called");
        }
        if (4 != tri.overload()) {
            fail("wrong overloaded method called");
        }
    }

    @Test
    public void testRegression13291() throws DBusException {
        SampleRemoteInterface tri = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface.class);

        logger.debug("reg13291...");
        byte[] as = new byte[10];
        for (int i = 0; i < 10; i++) {
            as[i] = (byte) (100 - i);
        }

        tri.reg13291(as, as);
        logger.debug("done");
    }

    @Test
    public void testDynamicObjectCreation() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);

        SampleNewInterface tni = tri2.getNew();

        assertEquals(tni.getName(), SampleNewInterfaceClass.class.getSimpleName());
    }

}
