package org.freedesktop.dbus.transport.jnr;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractUnixBusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;

public class JnrUnixBusAddress extends AbstractUnixBusAddress {

    public JnrUnixBusAddress(BusAddress _obj) throws TransportConfigurationException {
        super(_obj, true); // jnr-unixsocket supports abstract sockets
    }

}
