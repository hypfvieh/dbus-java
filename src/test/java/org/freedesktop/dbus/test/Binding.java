package org.freedesktop.dbus.test;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

/**
* Contains Binding-test interfaces
*/
public interface Binding {
    public interface SingleTests extends DBusInterface {
        @IntrospectionDescription("Returns the sum of the values in the input list")
        UInt32 Sum(byte[] a);
    }

    public interface TestClient extends DBusInterface {
        @IntrospectionDescription("when the trigger signal is received, this method should be called on the sending process/object.")
        void Response(UInt16 a, double b);

        @IntrospectionDescription("Causes a callback")
        class Trigger extends DBusSignal {
            public final UInt16 a;
            public final double b;

            public Trigger(String path, UInt16 _a, double _b) throws DBusException {
                super(path, _a, _b);
                this.a = _a;
                this.b = _b;
            }
        }

    }

    public interface Tests extends DBusInterface {
        @IntrospectionDescription("Returns whatever it is passed")
        <T> Variant<T> Identity(Variant<T> input);

        @IntrospectionDescription("Returns whatever it is passed")
        byte IdentityByte(byte input);

        @IntrospectionDescription("Returns whatever it is passed")
        boolean IdentityBool(boolean input);

        @IntrospectionDescription("Returns whatever it is passed")
        short IdentityInt16(short input);

        @IntrospectionDescription("Returns whatever it is passed")
        UInt16 IdentityUInt16(UInt16 input);

        @IntrospectionDescription("Returns whatever it is passed")
        int IdentityInt32(int input);

        @IntrospectionDescription("Returns whatever it is passed")
        UInt32 IdentityUInt32(UInt32 input);

        @IntrospectionDescription("Returns whatever it is passed")
        long IdentityInt64(long input);

        @IntrospectionDescription("Returns whatever it is passed")
        UInt64 IdentityUInt64(UInt64 input);

        @IntrospectionDescription("Returns whatever it is passed")
        double IdentityDouble(double input);

        @IntrospectionDescription("Returns whatever it is passed")
        String IdentityString(String input);

        @IntrospectionDescription("Returns whatever it is passed")
        <T> Variant<T>[] IdentityArray(Variant<T>[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        byte[] IdentityByteArray(byte[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        boolean[] IdentityBoolArray(boolean[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        short[] IdentityInt16Array(short[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        UInt16[] IdentityUInt16Array(UInt16[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        int[] IdentityInt32Array(int[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        UInt32[] IdentityUInt32Array(UInt32[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        long[] IdentityInt64Array(long[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        UInt64[] IdentityUInt64Array(UInt64[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        double[] IdentityDoubleArray(double[] input);

        @IntrospectionDescription("Returns whatever it is passed")
        String[] IdentityStringArray(String[] input);

        @IntrospectionDescription("Returns the sum of the values in the input list")
        long Sum(int[] a);

        @IntrospectionDescription("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
        Map<String, List<String>> InvertMapping(Map<String, String> a);

        @IntrospectionDescription("This method returns the contents of a struct as separate values")
        Binding.Triplet<String, UInt32, Short> DeStruct(Binding.TestStruct a);

        @IntrospectionDescription("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
        List<Variant<Object>> Primitize(Variant<Object> a);

        @IntrospectionDescription("inverts it's input")
        boolean Invert(boolean a);

        @IntrospectionDescription("triggers sending of a signal from the supplied object with the given parameter")
        void Trigger(String a, UInt64 b);

        @IntrospectionDescription("Causes the server to exit")
        void Exit();
    }

    public interface TestSignals extends DBusInterface {
        @IntrospectionDescription("Sent in response to a method call")
        class Triggered extends DBusSignal {
            public final UInt64 a;

            public Triggered(String _path, UInt64 _a) throws DBusException {
                super(_path, _a);
                this.a = _a;
            }
        }
    }

    final class Triplet<A, B, C> extends Tuple {
        @Position(0)
        public final A a;
        @Position(1)
        public final B b;
        @Position(2)
        public final C c;

        public Triplet(A _a, B _b, C _c) {
            this.a = _a;
            this.b = _b;
            this.c = _c;
        }
    }

    final class TestStruct extends Struct {
        @Position(0)
        public final String a;
        @Position(1)
        public final UInt32 b;
        @Position(2)
        public final Short  c;

        public TestStruct(String _a, UInt32 _b, Short _c) {
            this.a = _a;
            this.b = _b;
            this.c = _c;
        }
    }
}