/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.util.List;

import org.freedesktop.DBus.Description;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.DBusMemberName;
import org.freedesktop.dbus.Variant;

@Description("An example remote interface")
@DBusInterfaceName("org.freedesktop.dbus.test.AlternateTestInterface")
public interface TestRemoteInterface2 extends DBusInterface {
    @Description("Test multiple return values and implicit variant parameters.")
    <A> TestTuple<String, List<Integer>, Boolean> show(A in);

    @Description("Test passing structs and explicit variants, returning implicit variants")
    <T> T dostuff(TestStruct foo);

    @Description("Test arrays, boxed arrays and lists.")
    List<Integer> sampleArray(List<String> l, Integer[] is, long[] ls);

    @Description("Test passing objects as object paths.")
    DBusInterface getThis(DBusInterface t);

    @Description("Test bools work")
    @DBusMemberName("checkbool")
    boolean check();

    @Description("Test Serializable Object")
    TestSerializable<String> testSerializable(byte b, TestSerializable<String> s, int i);

    @Description("Call another method on itself from within a call")
    String recursionTest();

    @Description("Parameter-overloaded method (string)")
    int overload(String s);

    @Description("Parameter-overloaded method (byte)")
    int overload(byte b);

    @Description("Parameter-overloaded method (void)")
    int overload();

    @Description("Nested List Check")
    List<List<Integer>> checklist(List<List<Integer>> lli);

    @Description("Get new objects as object paths.")
    TestNewInterface getNew();

    @Description("Test Complex Variants")
    void complexv(Variant<? extends Object> v);

    //CHECKSTYLE:OFF
    @Description("Test Introspect on a different interface")
    String Introspect();
    //CHECKSTYLE:ON
}
