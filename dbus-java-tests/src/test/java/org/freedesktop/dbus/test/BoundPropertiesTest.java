package org.freedesktop.dbus.test;

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BoundPropertiesTest extends AbstractDBusBaseTest {

    @Test
    public void testProperties() throws IOException, DBusException, InterruptedException {
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
            MyObject obj = new MyObject();

            conn.requestBusName("com.acme");
            conn.exportObject(obj);

            try (DBusConnection innerConn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
                MyInterface myObject = innerConn.getRemoteObject("com.acme", "/com/acme/MyObject", MyInterface.class);

                assertEquals("Hello!", myObject.sayHello());
                assertEquals("Initial value", myObject.getMyProperty());
                assertFalse(myObject.isMyOtherProperty());
                assertEquals(123, myObject.getMyAltProperty());
                assertEquals(AnEnum.ABC, myObject.getAnEnum());
                assertArrayEquals(new String[] {"Item 1", "Item 2"}, myObject.getArrayOfStuff());
                assertEquals(Arrays.asList(1, 3, 5, 7, 11), myObject.getAList());
                assertEquals(Map.of("Key 1", 123L, "Key 2", Long.MAX_VALUE, "Key 3", Long.MIN_VALUE), myObject.getAMap());

                SampleStruct struct = myObject.getStruct();
                assertEquals("A", struct.getStringValue());
                assertEquals(new UInt32(123), struct.getInt32Value());
                assertEquals(new Variant<>(true), struct.getVariantValue());

                myObject.setMyProperty("New value");
                myObject.setMyOtherProperty(true);
                myObject.setMyAltProperty(987);
                myObject.setAnEnum(AnEnum.DEF);
                myObject.setArrayOfStuff(new String[] {"Another Item A", "Another Item B", "Another Item C"});
                SampleStruct struct2 = new SampleStruct("XXXX", new UInt32(999), new Variant<>(false));
                myObject.setStruct(struct2);

                myObject.setAList(Arrays.asList(999, 998, 997, 996));
                myObject.setAMap(Map.of("Key 4", 567L, "Key 5", Long.MAX_VALUE / 2, "Key 6", Long.MIN_VALUE / 2));

                assertEquals(myObject.getMyProperty(), "New value");
                assertTrue(myObject.isMyOtherProperty());
                assertEquals(myObject.getMyAltProperty(), 987);
                assertEquals(myObject.getAnEnum(), AnEnum.DEF);
                assertArrayEquals(new String[] {"Another Item A", "Another Item B", "Another Item C"}, myObject.getArrayOfStuff());
                struct2 = myObject.getStruct();
                assertEquals("XXXX", struct2.getStringValue());
                assertEquals(new UInt32(999), struct2.getInt32Value());
                assertEquals(new Variant<>(false), struct2.getVariantValue());

                assertEquals(Arrays.asList(999, 998, 997, 996), myObject.getAList());
                assertEquals(Map.of("Key 4", 567L, "Key 5", Long.MAX_VALUE / 2, "Key 6", Long.MIN_VALUE / 2), myObject.getAMap());

            }
        }
    }

    interface IntegerList extends TypeRef<List<Integer>> {

    }

    interface LongMap  extends TypeRef<Map<String, Long>> {

    }

    public enum AnEnum {
        ABC, DEF, GHI, JKL
    }

    @DBusInterfaceName("com.acme.MyInterface")
    public interface MyInterface extends DBusInterface {

        String sayHello();

        @DBusBoundProperty(access = Access.READ, name = "MyProperty")
        String getMyProperty();

        @DBusBoundProperty(access = Access.WRITE, name = "MyProperty")
        void setMyProperty(String _property);

        @DBusBoundProperty(access = Access.READ, name = "ZZZZZZZ")
        long getMyAltProperty();

        @DBusBoundProperty(access = Access.WRITE, name = "ZZZZZZZ")
        void setMyAltProperty(long _property);

        @DBusBoundProperty
        boolean isMyOtherProperty();

        @DBusBoundProperty
        void setMyOtherProperty(boolean  _property);

        @DBusBoundProperty
        AnEnum getAnEnum();

        @DBusBoundProperty
        void setAnEnum(AnEnum _anEnum);

        @DBusBoundProperty
        void setStruct(SampleStruct _struct);

        @DBusBoundProperty
        SampleStruct getStruct();

        @DBusBoundProperty
        String[] getArrayOfStuff();

        @DBusBoundProperty
        void setArrayOfStuff(String[] _arrayOfStuff);

        @DBusBoundProperty(type = LongMap.class)
        Map<String, Long> getAMap();

        @DBusBoundProperty(type = LongMap.class)
        void setAMap(Map<String, Long> _aMap);

        @DBusBoundProperty(type = IntegerList.class)
        List<Integer> getAList();

        @DBusBoundProperty(type = IntegerList.class)
        void setAList(List<Integer> _aList);
    }

    public class MyObject implements MyInterface {

        private String myProperty = "Initial value";
        private boolean myOtherProperty;
        private long myAltProperty = 123;
        private AnEnum anEnum = AnEnum.ABC;
        private SampleStruct struct = new SampleStruct("A", new UInt32(123), new Variant<>(true));
        private String[] arrayOfStuff = new String[] {"Item 1", "Item 2"};
        private Map<String, Long> aMap = Map.of("Key 1", 123L, "Key 2", Long.MAX_VALUE, "Key 3", Long.MIN_VALUE);
        private List<Integer> aList = Arrays.asList(1, 3, 5, 7, 11);

        @Override
        public List<Integer> getAList() {
            return aList;
        }

        @Override
        public void setAList(List<Integer> _aList) {
            this.aList = _aList;
        }

        @Override
        public SampleStruct getStruct() {
            return struct;
        }

        @Override
        public void setStruct(SampleStruct _struct) {
            this.struct = _struct;
        }

        @Override
        public AnEnum getAnEnum() {
            return anEnum;
        }

        @Override
        public void setAnEnum(AnEnum _anEnum) {
            this.anEnum = _anEnum;
        }

        @Override
        public String getObjectPath() {
            return "/com/acme/MyObject";
        }

        @Override
        public String sayHello() {
            return "Hello!";
        }

        @Override
        public long getMyAltProperty() {
            return myAltProperty;
        }

        @Override
        public void setMyAltProperty(long _myAltProperty) {
            this.myAltProperty = _myAltProperty;
        }

        @Override
        public String getMyProperty() {
            return myProperty;
        }

        @Override
        public void setMyProperty(String _property) {
            myProperty = _property;
        }

        @Override
        public boolean isMyOtherProperty() {
            return myOtherProperty;
        }

        @Override
        public void setMyOtherProperty(boolean _property) {
            myOtherProperty = _property;
        }

        @Override
        public boolean isRemote() {
            /* Whenever you are implementing an object, always return false */
            return false;
        }

        @Override
        public String[] getArrayOfStuff() {
            return arrayOfStuff;
        }

        @Override
        public void setArrayOfStuff(String[] _arrayOfStuff) {
            this.arrayOfStuff = _arrayOfStuff;
        }

        @Override
        public Map<String, Long> getAMap() {
            return aMap;
        }

        @Override
        public void setAMap(Map<String, Long> _aMap) {
            this.aMap = _aMap;
        }
    }
}
