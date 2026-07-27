package org.freedesktop.dbus.messages;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message.ConstructorArgType;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
            logger.debug("{} ---> {}", o[0], o[1]);
        }

        Object[] entry1 = (Object[]) objectList.getFirst();
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

    @Test
    void testPopulateIgnoresUnknownHeaderField() throws Exception {
        byte[] msg = {108, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0};

        // valid header field array, but the first field code (index 8)
        // is changed from 6 (DESTINATION) to 10 -> an unknown/out-of-range header field code.
        // Per the D-Bus specification unknown header fields must be ignored
        byte[] headers = {
                61, 0, 0, 0, 0, 0, 0, 0, 10, 1, 115, 0, 5, 0, 0, 0, 58, 49, 46, 50, 48, 0, 0, 0, 5, 1,
                117, 0, 1, 0, 0, 0, 8, 1, 103, 0, 1, 115, 0, 0, 7, 1, 115, 0, 20, 0, 0, 0, 111, 114,
                103, 46, 102, 114, 101, 101, 100, 101, 115, 107, 116, 111, 112, 46, 68, 66, 117, 115,
                0, 0, 0, 0
        };
        byte[] body = {};

        Message m = new Message();
        m.populate(msg, headers, body, null);

        // the unknown field (code 10, originally DESTINATION) must be skipped ...
        assertNull(m.getDestination(), "unknown header field must be ignored");
        // ... while all remaining valid fields are still parsed (a regression dropping fields would fail here)
        assertEquals(1L, m.getReplySerial(), "reply serial (field 5) should still be parsed");
        assertEquals("s", m.getSig(), "signature (field 8) should still be parsed");
        assertEquals("org.freedesktop.DBus", m.getSource(), "sender (field 7) should still be parsed");
    }

    @Test
    void testExtractArrayRejectsOversizedLength() {
        byte[] msg = {108, 1, 0, 1, 4, 0, 0, 0, 1, 0, 0, 0};

        // header field array, but the SIGNATURE field value (index 36-39)
        // is changed from "s" to "ay" -> the body is expected to be a byte array
        byte[] headers = {
                61, 0, 0, 0, 0, 0, 0, 0, 6, 1, 115, 0, 5, 0, 0, 0, 58, 49, 46, 50, 48, 0, 0, 0, 5, 1,
                117, 0, 1, 0, 0, 0, 8, 1, 103, 0, 2, 97, 121, 0, 7, 1, 115, 0, 20, 0, 0, 0, 111, 114,
                103, 46, 102, 114, 101, 101, 100, 101, 115, 107, 116, 111, 112, 46, 68, 66, 117, 115,
                0, 0, 0, 0
        };
        // body = a byte-array length field claiming ~4 GiB (0xFFFFFFFF); ensure this is handled properly
        byte[] body = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        Message m = new Message();
        assertDoesNotThrow(() -> m.populate(msg, headers, body, null));
        assertThrows(DBusException.class, m::getParameters);
    }

    @Test
    void testExtractRejectsDeeplyNestedVariants() {
        int nesting = 200; // far beyond Message.MAXIMUM_EXTRACT_DEPTH (64)

        // body: `nesting` nested variants ("v" in "v" in ...) ending in a single byte value
        byte[] body = new byte[nesting * 3 + 4];
        int i = 0;
        for (int d = 0; d < nesting; d++) {
            body[i++] = 1;    // signature length
            body[i++] = 'v';  // variant type code
            body[i++] = 0;    // nul terminator
        }
        body[i++] = 1;        // signature length
        body[i++] = 'y';      // byte type code (terminal)
        body[i++] = 0;        // nul terminator
        body[i] = 0x42;       // the byte value

        byte[] msg = {108, 1, 0, 1,
            (byte) body.length, (byte) (body.length >>> 8), (byte) (body.length >>> 16), (byte) (body.length >>> 24),
            1, 0, 0, 0};
        byte[] headers = headerWithSignature((byte) 'v');

        Message m = new Message();
        assertDoesNotThrow(() -> m.populate(msg, headers, body, null));
        DBusException ex = assertThrows(DBusException.class, m::getParameters);
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("nesting depth"),
            "expected nesting depth error, got: " + ex.getMessage());
    }

    @Test
    void testExtractFileDescriptorRejectsOutOfBoundsIndex() {
        // signature "h": body is a 4-byte fd index (0), but no file descriptors were received
        byte[] msg = {108, 1, 0, 1, 4, 0, 0, 0, 1, 0, 0, 0};
        byte[] headers = headerWithSignature((byte) 'h');
        byte[] body = {0, 0, 0, 0};

        Message m = new Message();
        assertDoesNotThrow(() -> m.populate(msg, headers, body, null));
        DBusException ex = assertThrows(DBusException.class, m::getParameters);
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("out of bounds"),
            "expected out-of-bounds error, got: " + ex.getMessage());
    }

    @Test
    void testPopulateRejectsUnixFdCountMismatch() {
        // header declares 2 unix fds (UNIX_FDS field 9), but none are actually received
        byte[] msg = {108, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0};
        byte[] headers = {
            8, 0, 0, 0,     // header array length (8 bytes of elements)
            0, 0, 0, 0,     // padding to 8-align the first struct
            9, 1, 117, 0,   // field UNIX_FDS(9), variant signature "u"
            2, 0, 0, 0      // UInt32 value = 2
        };
        byte[] body = {};

        Message m = new Message();
        DBusException ex = assertThrows(DBusException.class, () -> m.populate(msg, headers, body, null));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("unix file descriptors"),
            "expected fd count mismatch error, got: " + ex.getMessage());
    }

    /**
     * Returns a valid message header (as used by {@link #testReadMessageHeader()}) whose SIGNATURE field
     * value byte is replaced by the given type code.
     */
    private static byte[] headerWithSignature(byte _sigChar) {
        byte[] headers = {
                61, 0, 0, 0, 0, 0, 0, 0, 6, 1, 115, 0, 5, 0, 0, 0, 58, 49, 46, 50, 48, 0, 0, 0, 5, 1,
                117, 0, 1, 0, 0, 0, 8, 1, 103, 0, 1, 115, 0, 0, 7, 1, 115, 0, 20, 0, 0, 0, 111, 114,
                103, 46, 102, 114, 101, 101, 100, 101, 115, 107, 116, 111, 112, 46, 68, 66, 117, 115,
                0, 0, 0, 0
        };
        headers[37] = _sigChar; // SIGNATURE field value ('s' -> given type code)
        return headers;
    }

    static Stream<ParameterData> parameterSource() {
        return Stream.of(
            new ParameterData("Complex constructor", List.of(new Type[] {long.class, String.class, byte[].class, String.class, Map.class}, new Type[] {String.class}),
                List.of(Long.class, String.class, new DBusListType(Byte.class), String.class, new DBusMapType(CharSequence.class, Variant.class)),
                List.of(ConstructorArgType.NOT_ARRAY_TYPE, ConstructorArgType.NOT_ARRAY_TYPE, ConstructorArgType.PRIMITIVE_ARRAY,
                    ConstructorArgType.NOT_ARRAY_TYPE, ConstructorArgType.NOT_ARRAY_TYPE)),
            new ParameterData("Byte array constructor",
                List.of(new Type[] {Byte[].class, String.class}, new Type[] {Integer.class, String.class}),
                List.of(new DBusListType(Byte.class), String.class),
                List.of(ConstructorArgType.ARRAY,  ConstructorArgType.NOT_ARRAY_TYPE)),
            new ParameterData("Primitive Byte array constructor",
                List.of(new Type[] {byte[].class, String.class}, new Type[] {Integer.class, String.class}),
                List.of(new DBusListType(byte.class), String.class),
                List.of(ConstructorArgType.PRIMITIVE_ARRAY,  ConstructorArgType.NOT_ARRAY_TYPE)),
            new ParameterData("Byte array and List of Array constructor",
                List.of(new Type[] {byte[].class, String.class}, new Type[] {new DBusListType(Byte.class), String.class}),
                List.of(new DBusListType(byte.class), String.class),
                List.of(ConstructorArgType.PRIMITIVE_ARRAY,  ConstructorArgType.NOT_ARRAY_TYPE)), // if both variations are present, the first matching will be used
            new ParameterData("Byte array and different second argument",
                List.of(new Type[] {byte[].class, long.class}, new Type[] {byte[].class, String.class}),
                List.of(new DBusListType(byte.class), String.class),
                List.of(ConstructorArgType.PRIMITIVE_ARRAY, ConstructorArgType.NOT_ARRAY_TYPE)), // if both variations are present, the first matching will be used
            new ParameterData("Byte List constructor",
                List.of(new Type[] {List.class, int.class}, new Type[] {Long.class}),
                List.of(new DBusListType(Byte.class), int.class),
                List.of(ConstructorArgType.COLLECTION,  ConstructorArgType.NOT_ARRAY_TYPE)),
            new ParameterData("No arrays in constructor",
                List.of(new Type[] {Integer.class, String.class}, new Type[] {Integer.class, Integer.class}),
                List.of(String.class, String.class, Integer.class),
                List.of())
            );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterSource")
    void testExtractParameter(ParameterData _data) {
         assertEquals(_data.expected(), Message.usesPrimitives(_data.constructorArgs(), _data.wanted()));
    }

    record ParameterData(String name, List<Type[]> constructorArgs, List<Type> wanted, List<ConstructorArgType> expected) {
        @Override
        public String toString() {
            return name;
        }
    }
}
