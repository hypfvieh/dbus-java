package org.freedesktop.dbus.transport.tcp;

import java.net.ServerSocket;
import java.util.Random;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

public class TcpTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-tcp";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, int _timeout) throws TransportConfigurationException {
        return new TcpTransport(_address, _timeout);
    }

    @Override
    public String getSupportedBusType() {
        return "TCP";
    }

    @Override
    public String createDynamicSessionAddress(boolean _listeningSocket) {
        String address = "tcp:host=localhost";
        int port;
        try {
            ServerSocket s = new ServerSocket();
            s.bind(null);
            port = s.getLocalPort();
            s.close();
        } catch (Exception e) {
            Random r = new Random();
            port = 32768 + (Math.abs(r.nextInt()) % 28232);
        }
        address += ",port=" + port;
        if (_listeningSocket) {
            address += ",listen=true";
        }
        address += ",guid=" + Util.genGUID();
        LoggerFactory.getLogger(getClass()).debug("Created Session address: {}", address);
        return address;
    }

}
