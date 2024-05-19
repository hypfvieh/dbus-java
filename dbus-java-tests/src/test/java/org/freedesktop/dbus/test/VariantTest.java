package org.freedesktop.dbus.test;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.impl.BaseConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.spi.message.InputStreamMessageReader;
import org.freedesktop.dbus.spi.message.OutputStreamMessageWriter;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class VariantTest extends AbstractBaseTest {

    @Test
    void testVariant() {
        Variant<String> strVariant = new Variant<>("String");
        Variant<Integer> intVariant = new Variant<>(1);

        assertEquals("String", strVariant.getValue());
        assertEquals(String.class, strVariant.getType());

        assertEquals(1, intVariant.getValue());
        assertEquals(Integer.class, intVariant.getType());

        Variant<List<String>> listStrVariant = new Variant<>(List.of("str", "ing"), "as");
        Variant<List<Integer>> listIntVariant = new Variant<>(List.of(1, 2), "ai");

        assertEquals(List.of("str", "ing"), listStrVariant.getValue());
        assertEquals(List.class, ((DBusListType) listStrVariant.getType()).getRawType());

        assertEquals(List.of(1, 2), listIntVariant.getValue());
        assertEquals(List.class, ((DBusListType) listIntVariant.getType()).getRawType());
    }

    static List<VariantData> createTestData() {
        return List.of(
            new VariantData(new Variant<>(Map.of("val1", 1, "val2", 2), "a{si}"), "String Int Map", DBusMapType.class, LinkedHashMap.class, Map.of("val1", 1, "val2", 2)),
            new VariantData(new Variant<>(List.of("str", "ing"), "as"), "String List Variant", DBusListType.class, ArrayList.class, List.of("str", "ing")),
            new VariantData(new Variant<>(List.of(1, 2), "ai"), "Integer List Variant", DBusListType.class, ArrayList.class, List.of(1, 2)),
            new VariantData(new Variant<>(List.of(true, true, false), "ab"), "Boolean List Variant", DBusListType.class, ArrayList.class, List.of(true, true, false))
        );
    }

    /**
     * Checks if certain Variant flavors contain the correct datatype when deserialized.
     * <br>
     * This test is based on the problem that Variants containing a List or Collection of a type which
     * also has a primitive type were converted to array of primitive type on de-serialization.
     * <br>
     * Example: <br>
     * <ul>
     * <li>
     * Variant&lt;List&lt;String>> would be Variant&lt;List&lt;String>> on de-serialzation
     * because String has no primitive wrapper.
     * </li>
     * <li>
     * Variant&lt;List&lt;Integer>> will become int[] because the Collection is converted to array of
     * primitive type on de-serialization.
     * </li>
     * </ul>
     * <br>
     * The test ensures that if Variants are used, the returned value is always a List type not a primitive array type.
     *
     * @param _data
     * @throws DBusException
     * @throws IOException
     * @throws InterruptedException
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("createTestData")
    void testVariantSerializeDeserialize(VariantData _data) throws DBusException, IOException, InterruptedException {

        String dbusSig = Arrays.stream(Marshalling.getDBusType(_data.variantData.getClass()))
            .collect(Collectors.joining());

        logger.info("BusType {}: {}", _data.description(), dbusSig);

        MessageFactory factory = new MessageFactory(BaseConnectionBuilder.getSystemEndianness());
        MethodCall methodCall = factory.createMethodCall("variant.test.Tv", "/variant/test/Tv", "variant.test.Tv", "Sample", (byte) 0, dbusSig, _data.variantData());

        MethodReturn methodReturn = factory.createMethodReturn(methodCall, dbusSig, _data.variantData());

        Path unixPath = Path.of(System.getProperty("java.io.tmpdir"), getShortTestMethodName());
        Files.deleteIfExists(unixPath);

        var usx = UnixDomainSocketAddress.of(unixPath);

        CountDownLatch readWait = new CountDownLatch(1);
        CountDownLatch writeWait = new CountDownLatch(1);

        AtomicReference<Exception> ref = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try (var chan = ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(usx);
                var out = new OutputStreamMessageWriter(chan.accept())) {

                logger.debug("Sending --> {}", methodReturn);

                out.writeMessage(methodReturn);
                writeWait.countDown();
                readWait.await(10, TimeUnit.SECONDS);
            } catch (Exception _ex) {
                ref.set(_ex);
            }
        }, getTestMethodName() + "Thread");
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(300);
        try (var chan = SocketChannel.open(usx); var in = new InputStreamMessageReader(chan)) {
            writeWait.await(10, TimeUnit.SECONDS);
            Message message = in.readMessage();
            logger.debug("Receiving <-- {}", message);

            var t = ((Variant<?>) message.getParameters()[0]);

            assertEquals(_data.expectedType(), t.getType().getClass());
            assertEquals(_data.expectedValueClass(), t.getValue().getClass());
            assertEquals(_data.expected(), t.getValue());

            readWait.countDown();
        }
        if (ref.get() != null) {
            fail(ref.get());
        }
    }

    record VariantData(Variant<?> variantData, String description, Class<?> expectedType, Class<?> expectedValueClass, Object expected) {
        @Override
        public String toString() {
            return description;
        }
    }
}
