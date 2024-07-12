package org.freedesktop.dbus.test;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusTypeConversationRuntimeException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.messages.constants.MessageType;
import org.freedesktop.dbus.test.helper.structs.MarkTuple;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.test.helper.structs.SampleTuple;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;

class MarshallingTest extends AbstractBaseTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("createClassToSigData")
    void testJavaClassToDBusSignature(ClassToSigData _data) {
        assertThrows(DBusTypeConversationRuntimeException.class, () -> Marshalling.convertJavaClassesToSignature());

        assertEquals(_data.signature(), Marshalling.convertJavaClassesToSignature(_data.classes().toArray(Class[]::new)));
    }

    static List<ClassToSigData> createClassToSigData() {
        return List.of(
            new ClassToSigData("as", List.of(List.class, String.class), "List of String"),
            new ClassToSigData("ai", List.of(List.class, Integer.class), "List of Integer"),
            new ClassToSigData("as", List.of(Set.class, String.class), "Set of String"),
            new ClassToSigData("ai", List.of(Set.class, Integer.class), "Set of Integer"),
            new ClassToSigData("av", List.of(List.class, Variant.class), "List of Variant"),
            new ClassToSigData("a(suv)", List.of(List.class, SampleStruct.class), "List of SampleStruct"),
            new ClassToSigData("a", List.of(List.class, SampleTuple.class), "List of SampleTuple"),

            new ClassToSigData("aas", List.of(List.class, List.class, String.class), "List of List String"),
            new ClassToSigData("aai", List.of(List.class, List.class, Integer.class), "List of List Integer"),
            new ClassToSigData("a{si}", List.of(Map.class, String.class, Integer.class), "Map of String<>Integer"),
            new ClassToSigData("a{ii}", List.of(Map.class, Integer.class, Integer.class), "Map of Integer<>Integer"),
            new ClassToSigData("a{bv}", List.of(Map.class, Boolean.class, Variant.class), "Map of Boolean<>Variant")
        );
    }

    @Test
    void parseComplexMessageReturnsCorrectTypes() throws DBusException {
        List<Type> temp = new ArrayList<>();
        Marshalling.getJavaType("a(oa{sv})ao", temp, -1);

        assertEquals(2, temp.size(), "result must contain two types");
        assertTrue(temp.get(0) instanceof DBusListType);
        assertTrue(temp.get(1) instanceof DBusListType);
    }

    @Test
    void parseStructReturnsCorrectParsedCharsCount() throws Exception {
        List<Type> temp = new ArrayList<>();
        int parsedCharsCount = Marshalling.getJavaType("(oa{sv})ao", temp, 1);

        assertEquals(8, parsedCharsCount);
    }

    private static byte[] streamReader(String _file) throws IOException {
       return Files.readAllBytes(new File(_file).toPath());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMarshalling() throws Exception {

        // create an array with the required parameters of ServicesChanged signal (required to create a proper list of objects by reflection later on)
        Type[] types = null;
        if (null == types) {
            Constructor<?> con = IServicesChanged.ServicesChanged.class.getDeclaredConstructors()[0];
            Type[] ts = con.getGenericParameterTypes();
            types = new Type[ts.length - 1];
            for (int i = 1; i < ts.length; i++) {
                if (ts[i] instanceof TypeVariable) {
                    for (Type b : ((TypeVariable<GenericDeclaration>) ts[i]).getBounds()) {
                        types[i - 1] = b;
                    }
                } else {
                    types[i - 1] = ts[i];
                }
            }
        }

        // create a message from dumped data (including header and body)
        Message msg = MessageFactory.createMessage(MessageType.SIGNAL,
                streamReader("src/test/resources/" + getClass().getSimpleName() + "/connman_sample_buf.bin"),
                streamReader("src/test/resources/" + getClass().getSimpleName() + "/connman_sample_header.bin"),
                streamReader("src/test/resources/" + getClass().getSimpleName() + "/connman_sample_body.bin"), null);

        // use the Marshalling tools to get the parameters for the ServicesChanged signal
        Object[] params = Marshalling.deSerializeParameters(msg.getParameters(), types, null);

        // first parameter should be a list (of our custom type)
        assertTrue(params[0] instanceof List, "First param is not a List");
        // second parameter should be a list of object path
        assertTrue(params[1] instanceof List, "Second param is not a List");

    }

    @Test
    void testDeserializeParametersWithTuple() throws Exception {
        Object[] ob = {
                "rootfs.1", "marked slot rootfs.1 as good"
        };
        Method m = Installer.class.getDeclaredMethod("Mark", String.class, String.class);
        Type[] ts = new Type[] {m.getGenericReturnType()};

        Object[] params = Marshalling.deSerializeParameters(ob, ts, null);

        assertTrue(params[0] instanceof MarkTuple, "First param is not a MarkTuple");
        MarkTuple mt = (MarkTuple) params[0];
        assertEquals("rootfs.1", mt.getSlotName(), "Slot name does not match after deSerialization");
        assertEquals("marked slot rootfs.1 as good", mt.getMessage(), "Message does not match after deSerialization");
    }

    @Test
    void testDeserializeParametersVariant() throws Exception {
        var varList = new Variant<>(List.of(1, 2, 3), "ai");
        Type[] types = new Type[] {varList.getType()};

        Object[] convertParameters = Marshalling.convertParameters(new Object[] {varList}, types, new String[] {"v"}, null);

        Object[] params = Marshalling.deSerializeParameters(convertParameters, types, null);

        assertTrue(params[0] instanceof Variant, "Variant expected");
        @SuppressWarnings("unchecked")
        Variant<List<Integer>> mt = (Variant<List<Integer>>) params[0];
        assertEquals(1, mt.getValue().get(0), "1 expected");
        assertEquals(2, mt.getValue().get(1), "2 expected");
        assertEquals(3, mt.getValue().get(2), "3 expected");
    }

    @Test
    void testDeserializeMap() throws Exception {
        DBusPath key = new DBusPath("/foo");
        DBusPath val = new DBusPath("/bar");
        Map<DBusPath, DBusPath> testMap = Map.of(key, val);

        Type[] types = new Type[] {testMap.getClass()};

        Object[] deSerializeParameters = Marshalling.deSerializeParameters(new Object[] {testMap}, types, null);

        assertNotNull(deSerializeParameters);

        Map<?, ?> deserializedMap = (Map<?, ?>) deSerializeParameters[0];

        assertNotSame(testMap, deserializedMap, "Expected new object");
        assertEquals(LinkedHashMap.class, deserializedMap.getClass(), "Expected new LinkedHashMap");

        Entry<?, ?> next = deserializedMap.entrySet().iterator().next();

        assertEquals(key, next.getKey());
        assertEquals(val, next.getValue());

        assertTrue(deserializedMap.containsKey(key));
    }

    /*
     ******************************************
     *
     *     DUMMY TEST CLASSES
     *
     ******************************************
     */

    @DBusInterfaceName("net.connman.Manager")
    @SuppressWarnings({"checkstyle:methodname", "checkstyle:visibilitymodifier"})
    interface IServicesChanged extends DBusInterface {

         class ServicesChanged extends DBusSignal {

            final String  objectPath;

            final List<SomeData> changed;
            final List<ObjectPath> removed;

            ServicesChanged(String _objectPath, List<SomeData> _k, List<ObjectPath> _removedItems) throws DBusException {
                super(_objectPath, _k, _removedItems);
                objectPath = _objectPath;

                changed = _k;
                removed = _removedItems;
            }

            String getObjectPath() {
                return objectPath;
            }

            List<SomeData> getChanged() {
                return changed;
            }

            List<ObjectPath> getRemoved() {
                return removed;
            }

         }
    }

    public record SomeData(@Position(0) DBusPath objectPath, @Position(1) Map<String, Variant<?>> properties) {}

    @SuppressWarnings("checkstyle:methodname")
    public interface Installer extends DBusInterface {
        MarkTuple Mark(String _state, String _slotIdentifier);
    }

    public record ClassToSigData(String signature, List<Class<?>> classes, String description) {
        @Override
        public String toString() {
            return description;
        }
    }
}
