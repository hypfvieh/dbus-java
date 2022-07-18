package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.InvalidBusAddressException;
import org.junit.jupiter.api.Test;

public class NoTransportAddressTest {

    @Test
    public void testNoTransportAddress() {
        // this may only happen for TCP transports when no address is specified by system property
        if (TransportBuilder.getRegisteredBusTypes().contains("TCP")) {
            InvalidBusAddressException ex = assertThrows(InvalidBusAddressException.class, () -> DBusConnectionBuilder.forSessionBus().build());
            assertEquals("No valid TCP connection address found, please specify '" + AbstractConnection.TCP_ADDRESS_PROPERTY + "' system property", ex.getMessage());
        }
    }
}
