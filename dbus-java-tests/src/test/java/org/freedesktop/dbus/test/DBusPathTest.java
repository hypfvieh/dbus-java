package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.freedesktop.dbus.DBusPath;
import org.junit.jupiter.api.Test;

class DBusPathTest {

    @Test
    void testHashCode() {
        DBusPath p1 = new DBusPath("/bla");
        DBusPath p2 = new DBusPath("/bla");

        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
