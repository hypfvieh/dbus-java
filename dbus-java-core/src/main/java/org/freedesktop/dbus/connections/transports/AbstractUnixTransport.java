package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public abstract class AbstractUnixTransport extends AbstractTransport {

    protected AbstractUnixTransport(BusAddress _address) {
        super(_address);
    }

    public abstract int getUid(SocketChannel _sock) throws IOException;

}
