package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.exceptions.DBusException;

import java.util.List;

/**
 * Builder to create a new DirectConnection.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-04
 */
public final class DirectConnectionBuilder extends BaseConnectionBuilder<DirectConnectionBuilder, DirectConnection> {

    private DirectConnectionBuilder(BusAddress _address) {
        super(DirectConnectionBuilder.class, _address);
    }

    /**
     * Use the given address to create the connection (e.g. used for remote TCP connected DBus daemons).
     *
     * @param _address address to use
     * @return this
     */
    public static DirectConnectionBuilder forAddress(String _address) {
        List<BusAddress> addresses = BusAddress.parseAll(_address);
        DirectConnectionBuilder builder = new DirectConnectionBuilder(addresses.getFirst());
        builder.transportConfig().withBusAddresses(addresses);
        return builder;
    }

    /**
     * Use the given ordered list of addresses to create the connection. When connecting, the addresses are tried in
     * order until one succeeds (connect-fallback).
     *
     * @param _addresses candidate addresses, at least one required
     * @return this
     *
     * @since 6.0.0
     */
    public static DirectConnectionBuilder forAddresses(BusAddress... _addresses) {
        if (_addresses == null || _addresses.length == 0) {
            throw new IllegalArgumentException("At least one BusAddress is required");
        }
        List<BusAddress> addresses = List.of(_addresses);
        DirectConnectionBuilder builder = new DirectConnectionBuilder(addresses.getFirst());
        builder.transportConfig().withBusAddresses(addresses);
        return builder;
    }

    /**
     * Create the new {@link DBusConnection}.
     *
     * @return {@link DBusConnection}
     * @throws DBusException when DBusConnection could not be opened
     */
    @Override
    public DirectConnection build() throws DBusException {
        ReceivingServiceConfig rsCfg = buildThreadConfig();
        TransportConfig transportCfg = buildTransportConfig();
        ConnectionConfig connectionConfig = getConnectionConfig();

        return new DirectConnection(connectionConfig, transportCfg, rsCfg);
    }

}
