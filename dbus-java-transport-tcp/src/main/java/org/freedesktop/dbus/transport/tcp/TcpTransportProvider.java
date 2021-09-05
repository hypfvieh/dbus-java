package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;

public class TcpTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-tcp";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, int _timeout) throws TransportConfigurationException {
        return new TcpTransport(_address, _timeout);
    }

}
