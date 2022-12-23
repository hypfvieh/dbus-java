package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.interfaces.Binding.CrossSampleStruct;
import org.freedesktop.dbus.test.helper.interfaces.Binding.Triplet;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"checkstyle:methodname"})
public interface SamplesInterface extends DBusInterface {
    @IntrospectionDescription("Returns whatever it is passed")
    <T> Variant<T> Identity(Variant<T> _input);

    @IntrospectionDescription("Returns whatever it is passed")
    byte IdentityByte(byte _input);

    @IntrospectionDescription("Returns whatever it is passed")
    boolean IdentityBool(boolean _input);

    @IntrospectionDescription("Returns whatever it is passed")
    short IdentityInt16(short _input);

    @IntrospectionDescription("Returns whatever it is passed")
    UInt16 IdentityUInt16(UInt16 _input);

    @IntrospectionDescription("Returns whatever it is passed")
    int IdentityInt32(int _input);

    @IntrospectionDescription("Returns whatever it is passed")
    UInt32 IdentityUInt32(UInt32 _input);

    @IntrospectionDescription("Returns whatever it is passed")
    long IdentityInt64(long _input);

    @IntrospectionDescription("Returns whatever it is passed")
    UInt64 IdentityUInt64(UInt64 _input);

    @IntrospectionDescription("Returns whatever it is passed")
    double IdentityDouble(double _input);

    @IntrospectionDescription("Returns whatever it is passed")
    String IdentityString(String _input);

    @IntrospectionDescription("Returns whatever it is passed")
    <T> Variant<T>[] IdentityArray(Variant<T>[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    byte[] IdentityByteArray(byte[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    boolean[] IdentityBoolArray(boolean[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    short[] IdentityInt16Array(short[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    UInt16[] IdentityUInt16Array(UInt16[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    int[] IdentityInt32Array(int[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    UInt32[] IdentityUInt32Array(UInt32[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    long[] IdentityInt64Array(long[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    UInt64[] IdentityUInt64Array(UInt64[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    double[] IdentityDoubleArray(double[] _input);

    @IntrospectionDescription("Returns whatever it is passed")
    String[] IdentityStringArray(String[] _input);

    @IntrospectionDescription("Returns the sum of the values in the _input list")
    long Sum(int[] _a);

    @IntrospectionDescription("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
    Map<String, List<String>> InvertMapping(Map<String, String> _a);

    @IntrospectionDescription("This method returns the contents of a struct as separate values")
    Triplet<String, UInt32, Short> DeStruct(CrossSampleStruct _a);

    @IntrospectionDescription("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
    List<Variant<Object>> Primitize(Variant<Object> _a);

    @IntrospectionDescription("inverts it's input")
    boolean Invert(boolean _a);

    @IntrospectionDescription("triggers sending of a signal from the supplied object with the given parameter")
    void Trigger(String _a, UInt64 _b);

    @IntrospectionDescription("Causes the server to exit")
    void Exit();
}
