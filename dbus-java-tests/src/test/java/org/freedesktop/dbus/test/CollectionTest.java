package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.helper.NestedListCallbackHandler;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.test.helper.structs.IntStruct;
import org.freedesktop.dbus.test.helper.structs.SampleStruct4;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionTest extends AbstractDBusBaseTest {

    @Test
    public void testNestedListsAsync() throws DBusException, InterruptedException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        List<List<Integer>> lli = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        li.add(57);
        lli.add(li);

        @SuppressWarnings("unchecked")
        DBusAsyncReply<List<List<Integer>>> checklistReply = (DBusAsyncReply<List<List<Integer>>>) clientconn.callMethodAsync(tri2, "checklist",
                lli);

        // wait a bit to allow the async call to complete
        Thread.sleep(500L);

        assertIterableEquals(lli, checklistReply.getReply(), "did not get back the same as sent in async");
        assertIterableEquals(li, checklistReply.getReply().getFirst());
    }

    @Test
    public void testNestedListsCallback() throws DBusException, InterruptedException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        List<List<Integer>> lli = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        li.add(25);
        lli.add(li);

        NestedListCallbackHandler cbHandle = new NestedListCallbackHandler();

        clientconn.callWithCallback(tri2, "checklist", cbHandle, lli);

        // wait a bit to allow the async call to complete
        Thread.sleep(500L);

        assertIterableEquals(lli, cbHandle.getRetval(), "did not get back the same as sent in async");
        assertIterableEquals(li, cbHandle.getRetval().getFirst());
    }

    @Test
    public void testNestedLists() throws DBusException {
        SampleRemoteInterface2 tri2 = clientconn.getRemoteObject(getTestBusName(), getTestObjectPath(), SampleRemoteInterface2.class);
        List<List<Integer>> lli = new ArrayList<>();
        List<Integer> li = new ArrayList<>();
        li.add(1);
        lli.add(li);

        List<List<Integer>> reti = tri2.checklist(lli);
        if (reti.size() != 1 || reti.getFirst().size() != 1 || reti.getFirst().getFirst() != 1) {
            fail("Failed to check nested lists");
        }
    }

    @Test
    public void testListOfStruct() throws DBusException {
        SampleRemoteInterface tri = (SampleRemoteInterface) clientconn.getPeerRemoteObject(getTestBusName(), getTestObjectPath());

        IntStruct elem1 = new IntStruct(3, 7);
        IntStruct elem2 = new IntStruct(9, 14);
        List<IntStruct> list = Arrays.asList(elem1, elem2);
        SampleStruct4 param = new SampleStruct4(list);
        int[][] out = tri.testListstruct(param);
        if (out.length != 2) {
            fail("teststructstruct returned the wrong thing: " + Arrays.deepToString(out));
        }
        assertEquals(elem1.getValue1(), out[0][0]);
        assertEquals(elem1.getValue2(), out[0][1]);
        assertEquals(elem2.getValue1(), out[1][0]);
        assertEquals(elem2.getValue2(), out[1][1]);
    }
}
