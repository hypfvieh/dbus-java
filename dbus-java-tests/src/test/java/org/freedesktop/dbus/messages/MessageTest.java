package org.freedesktop.dbus.messages;

import java.util.List;

import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.types.UInt32;
import org.junit.jupiter.api.Test;

public class MessageTest extends AbstractBaseTest {

    @Test
    public void testReadMessageHeader() throws Exception {

        byte[] headerBytes = {
                61, 0, 0, 0, 0, 0, 0, 0, 6, 1, 115, 0, 5, 0, 0, 0, 58, 49, 46, 50, 48, 0, 0, 0, 5, 1,
                117, 0, 1, 0, 0, 0, 8, 1, 103, 0, 1, 115, 0, 0, 7, 1, 115, 0, 20, 0, 0, 0, 111, 114,
                103, 46, 102, 114, 101, 101, 100, 101, 115, 107, 116, 111, 112, 46, 68, 66, 117, 115,
                0, 0, 0, 0
        };

        Object[] extractHeader = new Message().extractHeader(headerBytes);

        assertEquals(1, extractHeader.length);
        assertInstanceOf(List.class, extractHeader[0]);

        List<?> objectList = (List<?>) extractHeader[0];
        assertEquals(4, objectList.size());

        for (Object object : objectList) {
            Object[] o = (Object[]) object;
            System.out.println(String.valueOf(o[0]) + " ---> " + o[1]);
        }

        Object[] entry1 = (Object[]) objectList.get(0);
        assertEquals((byte) 6, entry1[0]);
        assertEquals(":1.20", entry1[1]);

        Object[] entry2 = (Object[]) objectList.get(1);
        assertEquals((byte) 5, entry2[0]);
        assertEquals(new UInt32(1), entry2[1]);

        Object[] entry3 = (Object[]) objectList.get(2);
        assertEquals((byte) 8, entry3[0]);
        assertEquals("s", entry3[1]);

        Object[] entry4 = (Object[]) objectList.get(3);
        assertEquals((byte) 7, entry4[0]);
        assertEquals("org.freedesktop.DBus", entry4[1]);

    }

}
