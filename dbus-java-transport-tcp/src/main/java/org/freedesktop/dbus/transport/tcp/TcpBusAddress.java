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

    /**
     * Whether this is a {@code nonce-tcp} address (i.e. requires the nonce authentication handshake).
     *
     * @return true for nonce-tcp addresses
     */
    public boolean isNonceTcp() {
        return isBusType("nonce-tcp");
    }

    public boolean hasNonceFile() {
        return hasParameter("noncefile");
    }

    /**
     * The path to the nonce file as given by the {@code noncefile} address parameter (may be {@code null}).
     *
     * @return nonce file path or null
     */
    public String getNonceFile() {
        return getParameterValue("noncefile");
    }

}
