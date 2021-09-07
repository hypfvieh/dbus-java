package org.freedesktop.dbus.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.transports.AbstractTransport;

/**
 * Transport type representing a transport connection to TCP.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public class TcpTransport extends AbstractTransport {

    private SocketChannel socket;
    private final int     timeout;

    TcpTransport(BusAddress _address, int _timeout) {
        super(_address);
        timeout = _timeout;
        setSaslAuthMode(SASL.AUTH_SHA);
    }

    @Override
    protected boolean hasFileDescriptorSupport() {
        return false; // file descriptor passing not possible on TCP connections
    }

    /**
     * Connect to DBus using TCP.
     * @throws IOException on error
     */
    @Override
    public SocketChannel connectImpl() throws IOException {

        if (getAddress().isListeningSocket()) {

            try (ServerSocketChannel open = ServerSocketChannel.open()) {
                open.configureBlocking(true);
                open.bind(new InetSocketAddress(getAddress().getHost(), getAddress().getPort()));
                socket = open.accept();
            }
        } else {
            socket = SocketChannel.open();
            socket.configureBlocking(true);

            getLogger().trace("Setting timeout to {} on Socket", timeout);
            socket.socket().connect(new InetSocketAddress(getAddress().getHost(), getAddress().getPort()), timeout);
        }

        return socket;
    }

    @Override
    public void close() throws IOException {
        if (socket != null && socket.isOpen()) {
            socket.close();
        }
        super.close();
    }

    @Override
    protected boolean isAbstractAllowed() {
        return false;
    }
}

