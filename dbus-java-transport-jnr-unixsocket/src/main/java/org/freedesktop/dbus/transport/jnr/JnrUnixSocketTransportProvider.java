package org.freedesktop.dbus.transport.jnr;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;

public class JnrUnixSocketTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-jnr-unixsocket";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, int _timeout) throws TransportConfigurationException {
        return new UnixSocketTransport(_address);
    }

}
