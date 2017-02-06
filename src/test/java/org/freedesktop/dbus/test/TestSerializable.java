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
import java.util.Vector;

import org.freedesktop.dbus.DBusSerializable;
import org.freedesktop.dbus.exceptions.DBusException;

public class TestSerializable<A> implements DBusSerializable {
    private int             a;
    private String          b;
    private Vector<Integer> c;

    public TestSerializable(int _a, A _b, Vector<Integer> _c) {
        this.a = _a;
        this.b = _b.toString();
        this.c = _c;
    }

    public TestSerializable() {
    }

    public void deserialize(int _a, String _b, List<Integer> _c) {
        this.a = _a;
        this.b = _b;
        this.c = new Vector<Integer>(_c);
    }

    @Override
    public Object[] serialize() throws DBusException {
        return new Object[] {
                a, b, c
        };
    }

    public int getInt() {
        return a;
    }

    public String getString() {
        return b;
    }

    public Vector<Integer> getVector() {
        return c;
    }

    @Override
    public String toString() {
        return "TestSerializable{" + a + "," + b + "," + c + "}";
    }
}
