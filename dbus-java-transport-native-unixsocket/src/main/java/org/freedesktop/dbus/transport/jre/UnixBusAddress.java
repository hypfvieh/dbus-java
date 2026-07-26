package org.freedesktop.dbus.transport.jre;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractUnixBusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;

public class UnixBusAddress extends AbstractUnixBusAddress {

    public UnixBusAddress(BusAddress _obj) throws TransportConfigurationException {
        super(_obj, false); // native unix sockets do not support abstract sockets
    }

}
