package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusIgnore;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.annotations.MethodNoReply;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.structs.SampleStruct3;
import org.freedesktop.dbus.test.helper.structs.SampleStruct4;
import org.freedesktop.dbus.types.UInt16;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * A sample remote interface which exports one method.
 */
public interface SampleRemoteInterface extends DBusInterface {
    /**
    * A simple method with no parameters which returns a String
    */
    @IntrospectionDescription("Simple test method")
    String getName();

    String getNameAndThrow();

    @IntrospectionDescription("Test of nested maps")
    <T> int frobnicate(List<Long> _n, Map<String, Map<UInt16, Short>> _m, T _v);

    @IntrospectionDescription("Throws a TestException when called")
    void throwme() throws SampleException;

    @IntrospectionDescription("Waits then doesn't return")
    @MethodNoReply()
    void waitawhile();

    @IntrospectionDescription("Interface-overloaded method")
    int overload();

    @IntrospectionDescription("Testing Type Signatures")
    void sig(Type[] _s);

    @IntrospectionDescription("Testing object paths as Path objects")
    void newpathtest(DBusPath _p);

    @IntrospectionDescription("Testing the float type")
    float testfloat(float[] _f);

    @IntrospectionDescription("Testing structs of structs")
    int[][] teststructstruct(SampleStruct3 _in);

    @IntrospectionDescription("Regression test for #13291")
    void reg13291(byte[] _as, byte[] _bs);

    /* test lots of things involving Path */
    DBusPath pathrv(DBusPath _a);

    List<DBusPath> pathlistrv(List<DBusPath> _a);

    Map<DBusPath, DBusPath> pathmaprv(Map<DBusPath, DBusPath> _a);

    @IntrospectionDescription("Some function to test collections")
    int[][] testListstruct(SampleStruct4 _in);

    @DBusIgnore
    @IntrospectionDescription("This should not appear in introspection data, nor should it be callable remotely")
    void thisShouldBeIgnored();
}
