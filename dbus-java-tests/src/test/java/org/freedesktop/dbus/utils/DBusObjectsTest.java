package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.InvalidBusNameException;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

public class DBusObjectsTest extends AbstractBaseTest {

    @Test
    void testRequireBusName() {
        assertDoesNotThrow(() -> DBusObjects.requireBusName("org.freedesktop.DBus"));
        assertThrows(InvalidBusNameException.class, () -> DBusObjects.requireBusName(null));
        assertThrows(InvalidBusNameException.class, () -> DBusObjects.requireBusName(""));
    }

    @Test
    void testRequireNotBusName() {
        assertThrows(InvalidBusNameException.class, () -> DBusObjects.requireNotBusName("org.freedesktop.DBus", "test"));
        assertThrows(InvalidBusNameException.class, () -> DBusObjects.requireNotBusName(null, "test"));
        assertThrows(InvalidBusNameException.class, () -> DBusObjects.requireNotBusName("", "test"));
    }

    @Test
    void testRequireObjectPath() {
        assertDoesNotThrow(() -> DBusObjects.requireObjectPath(new DBusPath("/")));
        assertDoesNotThrow(() -> DBusObjects.requireObjectPath("/obj/path"));

        assertThrows(InvalidObjectPathException.class, () -> DBusObjects.requireObjectPath("blabla"));
        assertThrows(InvalidObjectPathException.class, () -> DBusObjects.requireObjectPath("bla/bla"));
    }

    @Test
    void testRequireBusNameOrConnectionId() {
        assertDoesNotThrow(() -> DBusObjects.requireBusNameOrConnectionId("org.freedesktop.DBus"));
        assertDoesNotThrow(() -> DBusObjects.requireBusNameOrConnectionId(":1.0"));
    }

    @Test
    void testRequireNotNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DBusObjects.requireNotNull(null, () -> new IllegalStateException("evil null")));
        assertEquals("evil null", ex.getMessage());
    }

}
