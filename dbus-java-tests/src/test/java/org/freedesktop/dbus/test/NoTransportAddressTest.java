package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.AddressResolvingException;
import org.freedesktop.dbus.utils.AddressBuilder;
import org.junit.jupiter.api.Test;

public class NoTransportAddressTest {

    @Test
    public void testNoTransportAddress() {
        if (TransportBuilder.getRegisteredBusTypes().contains("TCP")) {
            AddressResolvingException ex = assertThrows(AddressResolvingException.class, () -> DBusConnectionBuilder.forSessionBus().build());
            assertEquals("No transports found to handle UNIX socket connections. Please add a unix-socket transport provider to your classpath", ex.getMessage());
        } else if (TransportBuilder.getRegisteredBusTypes().contains("UNIX")) {
            System.setProperty(AddressBuilder.DBUS_SESSION_BUS_ADDRESS, "tcp:host=INVALID");
            AddressResolvingException ex = assertThrows(AddressResolvingException.class, () -> DBusConnectionBuilder.forSessionBus().build());
            assertEquals("No transports found to handle TCP connections. Please add a TCP transport provider to your classpath", ex.getMessage());
        }

    }
}
