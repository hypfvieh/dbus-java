package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSerializable;

import java.util.ArrayList;
import java.util.List;

public class SampleSerializable<A> implements DBusSerializable {
    private int             first;
    private String          second;
    private List<Integer>   third;

    public SampleSerializable(int _a, A _b, List<Integer> _c) {
        this.first = _a;
        this.second = _b.toString();
        this.third = _c;
    }

    public SampleSerializable() {
    }

    public void deserialize(int _a, String _b, List<Integer> _c) {
        this.first = _a;
        this.second = _b;
        this.third = new ArrayList<>(_c);
    }

    @Override
    public Object[] serialize() throws DBusException {
        return new Object[] {
                first, second, third
        };
    }

    public int getInt() {
        return first;
    }

    public String getString() {
        return second;
    }

    public List<Integer> getList() {
        return third;
    }

    @Override
    public String toString() {
        return "TestSerializable{" + first + "," + second + "," + third + "}";
    }
}
