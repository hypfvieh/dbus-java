package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractUnixTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.newsclub.net.unix.*;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class JUnixSocketUnixTransport extends AbstractUnixTransport {
    private final AFUNIXSocketAddress unixSocketAddress;
    private AFUNIXSocketChannel socket;
    private AFUNIXServerSocketChannel serverSocket;

    public JUnixSocketUnixTransport(JUnixSocketBusAddress _address, TransportConfig _config) throws TransportConfigurationException {
        super(_address, _config);

        StringBuilder path = new StringBuilder();
        if (_address.isAbstract()) {
            if (!AFSocket.supports(AFSocketCapability.CAPABILITY_ABSTRACT_NAMESPACE)) {
                throw new TransportConfigurationException("Abstract unix addresses not supported by current os");
            }
            path.append('\0');
            path.append(_address.getAbstract());
        } else if (_address.hasPath()) {
            path.append(_address.getPath());
        } else {
            throw new TransportConfigurationException("Unix socket url has to specify 'path' or 'abstract'");
        }

        try {
            unixSocketAddress = AFUNIXSocketAddress.of(path.toString().getBytes(Charset.defaultCharset()));
        } catch (SocketException _ex) {
            throw new TransportConfigurationException("Unable to resolve unix socket _address", _ex);
        }

        getSaslConfig().setAuthMode(SASL.AUTH_EXTERNAL);
    }

    @Override
    protected boolean hasFileDescriptorSupport() {
        return AFSocket.supports(AFSocketCapability.CAPABILITY_FILE_DESCRIPTORS) && AFSocket.supports(AFSocketCapability.CAPABILITY_UNSAFE);
    }

    @Override
    protected SocketChannel connectImpl() throws IOException {
        if (getAddress().isListeningSocket()) {
            throw new IOException("Connect connect to a listening socket (use listenImpl() instead)");
        }

        socket = AFUNIXSocketChannel.open();
        socket.configureBlocking(true);
        socket.connect(unixSocketAddress);

        socket.setAncillaryReceiveBufferSize(1024);

        return socket;
    }

    @Override
    public int getUid(SocketChannel _sock) throws IOException {
        if (_sock instanceof AFUNIXSocketExtensions) {
            AFUNIXSocketCredentials peerCredentials = ((AFUNIXSocketExtensions) _sock).getPeerCredentials();
            return (int) peerCredentials.getUid();
        }

        throw new IllegalArgumentException("Unable to handle unknown socket type: " + _sock.getClass());
    }

    @Override
    protected SocketChannel listenImpl() throws IOException {
        if (!getAddress().isListeningSocket()) {
            throw new IOException("Cannot listen on a client connection (use connectImpl() instead)");
        }

        if (serverSocket == null || !serverSocket.isOpen()) {
            serverSocket = AFUNIXServerSocketChannel.open();
            serverSocket.configureBlocking(true);
            serverSocket.bind(unixSocketAddress);
        }
        socket = serverSocket.accept();

        socket.setAncillaryReceiveBufferSize(1024);

        return socket;
    }

}
