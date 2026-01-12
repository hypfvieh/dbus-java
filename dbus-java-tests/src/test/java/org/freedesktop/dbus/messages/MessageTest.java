package org.freedesktop.dbus.messages;

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
            logger.debug("{} ---> {}", String.valueOf(o[0]), o[1]);
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
