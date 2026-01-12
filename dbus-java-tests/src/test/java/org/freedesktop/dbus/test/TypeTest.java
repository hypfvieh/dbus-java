package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterfaceEnum;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterfaceEnum.TestEnum;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.test.helper.structs.SampleStruct2;
import org.freedesktop.dbus.test.helper.structs.SampleStruct3;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeTest extends AbstractDBusBaseTest {
    @Test
    public void testEnum() throws DBusException {
        SampleRemoteInterfaceEnum tri = (SampleRemoteInterfaceEnum) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

        assertEquals(TestEnum.TESTVAL2, tri.getEnumValue());
    }

    @Test
    public void testArrays() throws DBusException {
        final SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);

        List<String> l = new ArrayList<>();
        l.add("hi");
        l.add("hello");
        l.add("hej");
        l.add("hey");
        l.add("aloha");
        logger.debug("Sampling Arrays:");
        List<Integer> is = tri2.sampleArray(l, new Integer[] {
                1, 5, 7, 9
        }, new long[] {
                2, 6, 8, 12
        });
        logger.debug("sampleArray returned an array:");
        for (Integer i : is) {
            logger.debug("--" + i);
        }

        assertEquals(5, is.size());
        assertEquals(-1, is.getFirst().intValue());
        assertEquals(-5, is.get(1).intValue());
        assertEquals(-7, is.get(2).intValue());
        assertEquals(-12, is.get(3).intValue());
        assertEquals(-18, is.get(4).intValue());
    }

    @Test
    public void testFloats() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

        logger.debug("sending it to sleep");
        tri.waitawhile();
        logger.debug("testing floats");
        if (17.093f != tri.testfloat(new float[] {
                17.093f, -23f, 0.0f, 31.42f
        })) {
            fail("testfloat returned the wrong thing");
        }
    }

    @Test
    public void testStruct() throws DBusException {
        final SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

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

    @Test
    public void testStructAsync() throws DBusException, InterruptedException {
        SampleRemoteInterface2 tri2 =
            clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        SampleStruct struct = new SampleStruct("fizbuzz", new UInt32(5248), new Variant<>(2234));

        @SuppressWarnings("unchecked")
        DBusAsyncReply<SampleStruct> structReply = (DBusAsyncReply<SampleStruct>) clientconn.callMethodAsync(tri2, "returnSamplestruct",
            struct);

        // wait a bit to allow the async call to complete
        Thread.sleep(500L);

        assertEquals(struct, structReply.getReply(), "struct did not match");
    }

    @Test
    public void testCallGetUtf8String() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());
        /** Call the remote object and get a response. */
        String rname = tri.getName();

        Collator col = Collator.getInstance();
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);

        if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!", rname)) {
            fail("getName return value incorrect");
        }
    }
}
