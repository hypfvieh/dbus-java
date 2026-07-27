package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Test interface with a method that blocks server-side so a client call stays pending (reply expected).
 */
public interface SlowInterface extends DBusInterface {
    String slowCall();
}
