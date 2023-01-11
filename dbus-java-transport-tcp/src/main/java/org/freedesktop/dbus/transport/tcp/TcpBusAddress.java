package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.utils.Util;

public class TcpBusAddress extends BusAddress {

    private static final int DEFAULT_PORT = 22839;

    public TcpBusAddress(BusAddress _obj) {
        super(_obj);
    }

    public String getHost() {
        return getParameters().get("host");
    }

    public boolean hasHost() {
        return getParameters().containsKey("host");
    }

    public boolean hasPort() {
        return getParameters().containsKey("port");
    }

    public int getPort() {
        return Util.isValidNetworkPort(getParameters().get("port"), true) ? Integer.parseInt(getParameters().get("port")) : DEFAULT_PORT;
    }

}
