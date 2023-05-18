package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Transport type representing a transport connection to TCP.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public class TcpTransport extends AbstractTransport {

    private final int           timeout;

    private SocketChannel       socket;
    private ServerSocketChannel serverSocket;

    TcpTransport(BusAddress _address, int _timeout, TransportConfig _config) {
        super(_address, _config);
        timeout = _timeout;
        getSaslConfig().setAuthMode(SASL.AUTH_SHA);
    }

    @Override
    protected boolean hasFileDescriptorSupport() {
        return false; // file descriptor passing not possible on TCP connections
    }

    @Override
    public TcpBusAddress getAddress() {
        return (TcpBusAddress) super.getAddress();
    }

    /**
     * Listen for new connections using TCP.
     *
     * @throws IOException on error
     */
    @Override
    protected SocketChannel listenImpl() throws IOException {
        if (!getAddress().isListeningSocket()) {
            throw new IOException("Cannot listen on client connections (use connectImpl() instead)");
        }

        if (serverSocket == null || !serverSocket.isOpen()) {
            InetSocketAddress socketAddress = new InetSocketAddress(getAddress().getHost(), getAddress().getPort());
            serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(true);
            getLogger().debug("Binding to {} using local port {}", getAddress().getHost(),
                getAddress().getPort(), getAddress().getPort());

            serverSocket.bind(socketAddress);
        }
        socket = serverSocket.accept();
        return socket;
    }

    /**
     * Connect to DBus using TCP.#
     *
     * @throws IOException on error
     */
    @Override
    public SocketChannel connectImpl() throws IOException {

        InetSocketAddress socketAddress = new InetSocketAddress(getAddress().getHost(), getAddress().getPort());
        if (getAddress().isListeningSocket()) {
            throw new IOException("Connect connect to a listening socket (use listenImpl() instead)");
        }

        socket = SocketChannel.open();
        socket.configureBlocking(true);

        getLogger().trace("Setting timeout to {} on Socket", timeout);
        socket.socket().connect(socketAddress, timeout);
        getLogger().debug("Connected to {} using local port {}", getAddress().getHost(),
            getAddress().getPort(), socket.socket().getLocalPort());

        return socket;
    }

    @Override
    public void close() throws IOException {
        getLogger().debug("Disconnecting Transport: {}", this);

        super.close();

        if (socket != null && socket.isOpen()) {
            socket.close();
        }

        if (serverSocket != null && serverSocket.isOpen()) {
            serverSocket.close();
        }
    }

}

