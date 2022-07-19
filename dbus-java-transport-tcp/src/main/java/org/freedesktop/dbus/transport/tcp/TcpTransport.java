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

    private final int           timeout;

    private SocketChannel       socket;
    private ServerSocketChannel serverSocket;

    TcpTransport(BusAddress _address, int _timeout) {
        super(_address);
        timeout = _timeout;
        setSaslAuthMode(SASL.AUTH_SHA);
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
     * Connect to DBus using TCP.
     * @throws IOException on error
     */
    @Override
    public SocketChannel connectImpl() throws IOException {

        InetSocketAddress socketAddress = new InetSocketAddress(getAddress().getHost(), getAddress().getPort());
        if (getAddress().isListeningSocket()) {
            if (serverSocket == null || !serverSocket.isOpen()) {
                serverSocket = ServerSocketChannel.open();
                serverSocket.configureBlocking(true);
                serverSocket.bind(socketAddress);
            }
            socket = serverSocket.accept();
        } else {
            socket = SocketChannel.open();
            socket.configureBlocking(true);

            getLogger().trace("Setting timeout to {} on Socket", timeout);
            socket.socket().connect(socketAddress, timeout);
        }

        return socket;
    }

    @Override
    public void close() throws IOException {
        getLogger().debug("Disconnecting Transport");

        super.close();

        if (socket != null && socket.isOpen()) {
            socket.close();
        }

        if (serverSocket != null && serverSocket.isOpen()) {
            serverSocket.close();
        }
    }

    @Deprecated
    @Override
    protected boolean isAbstractAllowed() {
        return false;
    }
}

