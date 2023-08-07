package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;
import org.newsclub.net.unix.AFSocket;
import org.newsclub.net.unix.AFSocketCapability;

public class JUnixSocketTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-junixsocket";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, TransportConfig _config) throws TransportConfigurationException {
        if (!AFSocket.supports(AFSocketCapability.CAPABILITY_UNIX_DOMAIN)) {
            return null;
        }

        JUnixSocketBusAddress unixBusAddress;
        if (_address instanceof JUnixSocketBusAddress) {
            unixBusAddress = (JUnixSocketBusAddress) _address;
        } else {
            unixBusAddress = new JUnixSocketBusAddress(_address);
        }

        return new JUnixSocketUnixTransport(unixBusAddress, _config);
    }

    @Override
    public String getSupportedBusType() {
        return "UNIX";
    }

    @Override
    public String createDynamicSessionAddress(boolean _listeningSocket) {
        return Util.createDynamicSessionAddress(_listeningSocket, false);
    }

}
