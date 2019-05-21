/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.jupiter.api.Test;

/**
 * 
 */
public class DBusConnectionTest {

    @Test
    public void test_busnames_should_be_auto_released_on_close_of_non_shared_connection() throws Exception {

        // prepare 2 independent connection
        String busName = "org.freedesktop.dbus.test.TestBus";
        DBusConnection connection1 = DBusConnection.getConnection(DBusBusType.SESSION, false, DBusConnection.TCP_CONNECT_TIMEOUT);
        DBusConnection connection2 = DBusConnection.getConnection(DBusBusType.SESSION, false, DBusConnection.TCP_CONNECT_TIMEOUT);
        assertNotEquals(connection1.getUniqueName(), connection2.getUniqueName());

        // only one connection can have the bus
        connection1.requestBusName(busName);
        assertThrows(DBusException.class, () -> connection2.requestBusName(busName));
        
        // after a close of conn1 the bus name should be available again
        connection1.close();
        connection2.requestBusName(busName);
        connection2.close();
    }
}
