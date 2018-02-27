/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;

/**
 * A sample signal with two parameters
 */
@IntrospectionDescription("Test interface containing signals")
@DBusInterfaceName("some.other.interface.Name")
public interface TestSignalInterface2 extends DBusInterface {
    @IntrospectionDescription("Test basic signal")
    class TestRenamedSignal extends DBusSignal {
        //CHECKSTYLE:OFF
        public final String value;
        public final UInt32 number;
        //CHECKSTYLE:ON
        /**
         * Create a signal.
         */
        public TestRenamedSignal(String _path, String _value, UInt32 _number) throws DBusException {
            super(_path, _value, _number);
            this.value = _value;
            this.number = _number;
        }
    }
}
