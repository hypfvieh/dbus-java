package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractUnixBusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.newsclub.net.unix.AFSocket;
import org.newsclub.net.unix.AFSocketCapability;

public class JUnixSocketBusAddress extends AbstractUnixBusAddress {

    public JUnixSocketBusAddress(BusAddress _busAddress) throws TransportConfigurationException {
        super(_busAddress, AFSocket.supports(AFSocketCapability.CAPABILITY_ABSTRACT_NAMESPACE));
    }

}
