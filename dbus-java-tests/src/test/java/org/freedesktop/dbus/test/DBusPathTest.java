package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.ObjectPath;
import org.junit.jupiter.api.Test;

public class DBusPathTest {

    @Test
    void testEquals() {
        DBusPath p1 = new DBusPath("/bla");
        DBusPath p2 = new DBusPath("/bla");

        ObjectPath o1 = new ObjectPath("/bla", null);

        assertEquals(p1, p2);
        assertNotEquals(p1, o1);
    }

    @Test
    void testHashCode() {
        DBusPath p1 = new DBusPath("/bla");
        DBusPath p2 = new DBusPath("/bla");

        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
