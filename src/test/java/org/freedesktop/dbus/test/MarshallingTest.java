package org.freedesktop.dbus.test;

import static java.lang.Byte.parseByte;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MarshallingTest {

    private static final String SIGNATURE = "a(oa{sv})ao";
    //private static final String SIGNATURE = "a(o{sv})a(o)";

    private static byte[] content;

    @BeforeAll
    public static void init() {
        InputStream resourceAsStream = MarshallingTest.class.getResourceAsStream("/marshallingtest.txt");
        Scanner s = new Scanner(resourceAsStream);
        s.useDelimiter(", ");
        List<Byte> byteList = new ArrayList<>();
        while (s.hasNext()) {
            byteList.add(parseByte(s.next()));
        }
        content = new byte[byteList.size()];
        for (int idx = 0; idx < byteList.size(); idx++) {
            content[idx] = byteList.get(idx);
        }
        s.close();
    }

    @Test
    public void testMarshalling() throws DBusException {
        Message msg = new Message(Message.Endian.LITTLE, (byte) 4, (byte) 0) {
        };

        Object[] params = msg.extract(SIGNATURE, content, 0);

        assertTrue(params[0] instanceof List, "First param is not a List");
        assertTrue(params[1] instanceof List, "Second param is not a List");
    }
}
