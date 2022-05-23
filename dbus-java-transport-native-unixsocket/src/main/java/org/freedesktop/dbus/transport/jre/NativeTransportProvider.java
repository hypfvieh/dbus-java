package org.freedesktop.dbus.transport.jre;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;

public class NativeTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-native-unixsocket";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, int _timeout) throws TransportConfigurationException {
        return new NativeUnixSocketTransport(_address);
    }

    @Override
    public String getSupportedBusType() {
        return "UNIX";
    }

    @Override
    public String createDynamicSessionAddress(boolean _listeningSocket) {
        return Util.createDynamicSessionAddress(_listeningSocket, false); // native unix sockets do not support abstract sockets
    }

}
