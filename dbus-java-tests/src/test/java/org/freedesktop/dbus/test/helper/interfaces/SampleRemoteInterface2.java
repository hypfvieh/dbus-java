package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.SampleSerializable;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.test.helper.structs.SampleTuple;
import org.freedesktop.dbus.types.Variant;

import java.util.List;

@IntrospectionDescription("An example remote interface")
@DBusInterfaceName("org.freedesktop.dbus.test.AlternateTestInterface")
@SuppressWarnings({"checkstyle:methodname"})
public interface SampleRemoteInterface2 extends DBusInterface {
    @IntrospectionDescription("Test multiple return values and implicit variant parameters.")
    <A> SampleTuple<String, List<Integer>, Boolean> show(A _in);

    @IntrospectionDescription("Test passing structs and explicit variants, returning implicit variants")
    <T> T dostuff(SampleStruct _foo);

    @IntrospectionDescription("Test arrays, boxed arrays and lists.")
    List<Integer> sampleArray(List<String> _l, Integer[] _is, long[] _ls);

    @IntrospectionDescription("Test passing objects as object paths.")
    DBusInterface getThis(DBusInterface _t);

    @IntrospectionDescription("Test bools work")
    @DBusMemberName("checkbool")
    boolean check();

    @IntrospectionDescription("Test Serializable Object")
    SampleSerializable<String> testSerializable(byte _b, SampleSerializable<String> _s, int _i);

    @IntrospectionDescription("Call another method on itself from within a call")
    String recursionTest(String _dbusName, String _path);

    @IntrospectionDescription("Parameter-overloaded method (string)")
    int overload(String _s);

    @IntrospectionDescription("Parameter-overloaded method (byte)")
    int overload(byte _b);

    @IntrospectionDescription("Parameter-overloaded method (void)")
    int overload();

    @IntrospectionDescription("Nested List Check")
    List<List<Integer>> checklist(List<List<Integer>> _lli);

    @IntrospectionDescription("Get new objects as object paths.")
    SampleNewInterface getNew();

    @IntrospectionDescription("Test Complex Variants")
    void complexv(Variant<? extends Object> _v);

    @IntrospectionDescription("Test Introspect on a different interface")
    String Introspect();

    @IntrospectionDescription("Returns the given struct")
    SampleStruct returnSamplestruct(SampleStruct _struct);
}
