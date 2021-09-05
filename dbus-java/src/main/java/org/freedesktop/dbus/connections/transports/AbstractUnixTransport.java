package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.freedesktop.dbus.connections.BusAddress;

public abstract class AbstractUnixTransport extends AbstractTransport {

    protected AbstractUnixTransport(BusAddress _address) {
        super(_address);
    }

    public abstract int getUid(SocketChannel _sock) throws IOException;

}
