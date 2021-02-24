package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * A sample remote interface which exports one method.
 */
public interface SampleNewInterface extends DBusInterface {
    /**
    * A simple method with no parameters which returns a String
    */
    @IntrospectionDescription("Simple test method")
    String getName();
}
