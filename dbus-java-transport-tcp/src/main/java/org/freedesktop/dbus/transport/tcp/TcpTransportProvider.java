package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.util.Random;

public class TcpTransportProvider implements ITransportProvider {
    public static final int TCP_CONNECT_TIMEOUT     = 100000;

    @Override
    public String getTransportName() {
        return "dbus-java-transport-tcp";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, TransportConfig _config) throws TransportConfigurationException {
        TcpBusAddress address = null;
        if (_address instanceof TcpBusAddress) {
            address = (TcpBusAddress) _address;
        } else {
            address = new TcpBusAddress(_address);
        }

        int timeout = _config.getTimeout();

        return new TcpTransport(address, timeout, _config);
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
        } catch (Exception _ex) {
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
