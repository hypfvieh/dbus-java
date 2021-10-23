package org.freedesktop.dbus.transport.jnr;

import java.io.File;
import java.util.Random;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

public class JnrUnixSocketTransportProvider implements ITransportProvider {

    @Override
    public String getTransportName() {
        return "dbus-java-transport-jnr-unixsocket";
    }

    @Override
    public AbstractTransport createTransport(BusAddress _address, int _timeout) throws TransportConfigurationException {
        return new UnixSocketTransport(_address);
    }

    @Override
    public String getSupportedBusType() {
        return "UNIX";
    }

    @Override
    public String createDynamicSessionAddress(boolean _listeningSocket) {
        String address = "unix:";
        String path = new File(System.getProperty("java.io.tmpdir"), "dbus-XXXXXXXXXX").getAbsolutePath();
        Random r = new Random();
        do {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 10; i++) {
                sb.append((char) ((Math.abs(r.nextInt()) % 26) + 65));
            }
            path = path.replaceAll("..........$", sb.toString());
            LoggerFactory.getLogger(getClass()).trace("Trying path {}", path);
        } while ((new File(path)).exists());
        if (Util.isFreeBsd() || Util.isMacOs()) {
            address += "path=" + path;
        } else {
            address += "abstract=" + path;
        }

        if (_listeningSocket) {
            address += ",listen=true";
        }

        address += ",guid=" + Util.genGUID();
        LoggerFactory.getLogger(getClass()).debug("Created Session address: {}", address);
        return address;
    }

}
