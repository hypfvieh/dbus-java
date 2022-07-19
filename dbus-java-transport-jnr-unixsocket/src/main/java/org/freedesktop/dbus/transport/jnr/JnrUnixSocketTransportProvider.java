package org.freedesktop.dbus.transport.jnr;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;

public class JnrUnixSocketTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-jnr-unixsocket";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, int _timeout) throws TransportConfigurationException {
        JnrUnixBusAddress address = null;
        if (_address instanceof JnrUnixBusAddress) {
            address = (JnrUnixBusAddress) _address;
        } else {
            address = new JnrUnixBusAddress(_address);
        }
        return new UnixSocketTransport(address);
    }

    @Override
    public String getSupportedBusType() {
        return "UNIX";
    }

    @Override
    public String createDynamicSessionAddress(boolean _listeningSocket) {
        return Util.createDynamicSessionAddress(_listeningSocket, Util.isFreeBsd() || Util.isMacOs());
    }

}
