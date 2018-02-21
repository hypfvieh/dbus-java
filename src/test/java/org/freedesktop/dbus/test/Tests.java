package org.freedesktop.dbus.test;

import java.util.List;
import java.util.Map;

import org.freedesktop.DBus.Description;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.test.Binding.Triplet;

public interface Tests extends DBusInterface {
    @Description("Returns whatever it is passed")
    <T> Variant<T> Identity(Variant<T> input);

    @Description("Returns whatever it is passed")
    byte IdentityByte(byte input);

    @Description("Returns whatever it is passed")
    boolean IdentityBool(boolean input);

    @Description("Returns whatever it is passed")
    short IdentityInt16(short input);

    @Description("Returns whatever it is passed")
    UInt16 IdentityUInt16(UInt16 input);

    @Description("Returns whatever it is passed")
    int IdentityInt32(int input);

    @Description("Returns whatever it is passed")
    UInt32 IdentityUInt32(UInt32 input);

    @Description("Returns whatever it is passed")
    long IdentityInt64(long input);

    @Description("Returns whatever it is passed")
    UInt64 IdentityUInt64(UInt64 input);

    @Description("Returns whatever it is passed")
    double IdentityDouble(double input);

    @Description("Returns whatever it is passed")
    String IdentityString(String input);

    @Description("Returns whatever it is passed")
    <T> Variant<T>[] IdentityArray(Variant<T>[] input);

    @Description("Returns whatever it is passed")
    byte[] IdentityByteArray(byte[] input);

    @Description("Returns whatever it is passed")
    boolean[] IdentityBoolArray(boolean[] input);

    @Description("Returns whatever it is passed")
    short[] IdentityInt16Array(short[] input);

    @Description("Returns whatever it is passed")
    UInt16[] IdentityUInt16Array(UInt16[] input);

    @Description("Returns whatever it is passed")
    int[] IdentityInt32Array(int[] input);

    @Description("Returns whatever it is passed")
    UInt32[] IdentityUInt32Array(UInt32[] input);

    @Description("Returns whatever it is passed")
    long[] IdentityInt64Array(long[] input);

    @Description("Returns whatever it is passed")
    UInt64[] IdentityUInt64Array(UInt64[] input);

    @Description("Returns whatever it is passed")
    double[] IdentityDoubleArray(double[] input);

    @Description("Returns whatever it is passed")
    String[] IdentityStringArray(String[] input);

    @Description("Returns the sum of the values in the input list")
    long Sum(int[] a);

    @Description("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
    Map<String, List<String>> InvertMapping(Map<String, String> a);

    @Description("This method returns the contents of a struct as separate values")
    Triplet<String, UInt32, Variant<?>> DeStruct(TestStruct a);

    @Description("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
    List<Variant<Object>> Primitize(Variant<Object> a);

    @Description("inverts it's input")
    boolean Invert(boolean a);

    @Description("triggers sending of a signal from the supplied object with the given parameter")
    void Trigger(String a, UInt64 b);

    @Description("Causes the server to exit")
    void Exit();
}