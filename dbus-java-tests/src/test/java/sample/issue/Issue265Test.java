package sample.issue;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class Issue265Test extends AbstractBaseTest {

    @Test
    public void testNestedStruct() throws DBusException {
        String[] dBusType = Marshalling.getDBusType(Struct1.class);

        // Every struct has 2 bytes signature: open/closing bracket
        // Struct1 has one additional int member
        // this test uses nested structs with a depth of 15
        // => 3 * 15 bytes should be in signature string at the end
        int expectedSigLen = 3 * 15;

        assertEquals(1, dBusType.length);
        assertEquals(expectedSigLen, dBusType[0].length());
        assertEquals("[(i(i(i(i(i(i(i(i(i(i(i(i(i(i(i)))))))))))))))]", Arrays.toString(dBusType));
    }

    public static class Struct1 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct2 inner;
    }

    public static class Struct2 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct3 inner;
    }

    public static class Struct3 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct4 inner;
    }

    public static class Struct4 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct5 inner;
    }

    public static class Struct5 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct6 inner;
    }

    public static class Struct6 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct7 inner;
    }

    public static class Struct7 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct8 inner;
    }

    public static class Struct8 extends Struct {
        @Position(0)
        private int     foo;

        @Position(1)
        private Struct9 inner;
    }

    public static class Struct9 extends Struct {
        @Position(0)
        private int      foo;

        @Position(1)
        private Struct10 inner;
    }

    public static class Struct10 extends Struct {
        @Position(0)
        private int      foo;

        @Position(1)
        private Struct11 inner;
    }

    public static class Struct11 extends Struct {
        @Position(0)
        private int      foo;

        @Position(1)
        private Struct12 inner;
    }

    public static class Struct12 extends Struct {
        @Position(0)
        private int      foo;

        @Position(1)
        private Struct13 inner;
    }

    public static class Struct13 extends Struct {
        @Position(0)
        private int      foo;

        @Position(1)
        private Struct14 inner;
    }

    public static class Struct14 extends Struct {
        @Position(0)
        private int      foo;

        @Position(1)
        private Struct15 inner;
    }

    public static class Struct15 extends Struct {
        @Position(0)
        private int foo;

    }
}
