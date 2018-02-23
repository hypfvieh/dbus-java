/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * A sample remote interface which exports one method.
 */
public interface TestNewInterface extends DBusInterface {
    /**
    * A simple method with no parameters which returns a String
    */
    @IntrospectionDescription("Simple test method")
    String getName();
}
