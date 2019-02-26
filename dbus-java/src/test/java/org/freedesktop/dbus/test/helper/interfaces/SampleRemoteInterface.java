/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.helper.interfaces;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.annotations.MethodNoReply;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.SampleException;
import org.freedesktop.dbus.test.helper.structs.SampleStruct3;
import org.freedesktop.dbus.test.helper.structs.SampleStruct4;
import org.freedesktop.dbus.types.UInt16;

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
    <T> int frobnicate(List<Long> n, Map<String, Map<UInt16, Short>> m, T v);

    @IntrospectionDescription("Throws a TestException when called")
    void throwme() throws SampleException;

    @IntrospectionDescription("Waits then doesn't return")
    @MethodNoReply()
    void waitawhile();

    @IntrospectionDescription("Interface-overloaded method")
    int overload();

    @IntrospectionDescription("Testing Type Signatures")
    void sig(Type[] s);

    @IntrospectionDescription("Testing object paths as Path objects")
    void newpathtest(DBusPath p);

    @IntrospectionDescription("Testing the float type")
    float testfloat(float[] f);

    @IntrospectionDescription("Testing structs of structs")
    int[][] teststructstruct(SampleStruct3 in);

    @IntrospectionDescription("Regression test for #13291")
    void reg13291(byte[] as, byte[] bs);

    /* test lots of things involving Path */
    DBusPath pathrv(DBusPath a);

    List<DBusPath> pathlistrv(List<DBusPath> a);

    Map<DBusPath, DBusPath> pathmaprv(Map<DBusPath, DBusPath> a);

    @IntrospectionDescription("Some function to test collections")
	int[][] testListstruct(SampleStruct4 in);
}
