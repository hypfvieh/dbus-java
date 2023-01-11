package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.utils.Util;

public class TcpBusAddress extends BusAddress {

    private static final int DEFAULT_PORT = 22839;

    public TcpBusAddress(BusAddress _obj) {
        super(_obj);
    }

    public String getHost() {
        return getParameterValue("host");
    }

    public boolean hasHost() {
        return hasParameter("host");
    }

    public boolean hasPort() {
        return hasParameter("port");
    }

    public int getPort() {
        return Util.isValidNetworkPort(getParameterValue("port"), true) ? Integer.parseInt(getParameterValue("port")) : DEFAULT_PORT;
    }

}
