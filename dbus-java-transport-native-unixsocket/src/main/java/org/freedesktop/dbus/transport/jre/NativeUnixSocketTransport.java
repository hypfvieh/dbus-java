package org.freedesktop.dbus.transport.jre;


import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.transports.AbstractUnixTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;

/**
 * Transport type representing a transport connection to a unix socket.
 * This implementation uses features of Java 16+ to connect to a unix
 * socket without a 3rd party library.
 * <p>
 * Please note: The functionality of the native unix sockets in Java are
 * limited. 'Side-channel' communication (e.g. passing file descriptors)
 * is not possible (unlike using jnr-unix socket + dbus-java-nativefd).
 * <br><br>
 * Also using 'abstract' sockets is not possible when using this native implementation.
 * <br>
 * In most cases this implementation should suit our needs.
 * If it does not fit for you, use jnr-unixsocket instead.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-01
 */
public class NativeUnixSocketTransport extends AbstractUnixTransport {
    private final UnixDomainSocketAddress unixSocketAddress;
    private SocketChannel                 socket;
    private ServerSocketChannel           serverSocket;

    NativeUnixSocketTransport(BusAddress _address) throws TransportConfigurationException {
        super(_address);

        if (_address.isAbstract()) {
            throw new TransportConfigurationException("Abstract sockets are not supported using java native unix sockets");
        } else if (_address.hasPath()) {
            unixSocketAddress = UnixDomainSocketAddress.of(_address.getPath());
        } else {
            throw new TransportConfigurationException("Native unix socket url has to specify 'path'");
        }

        setSaslAuthMode(SASL.AUTH_EXTERNAL);
    }

    @Override
    protected boolean hasFileDescriptorSupport() {
        return true; // file descriptor passing allowed when using UNIX_SOCK
    }

    /**
     * Establish a connection to DBus using unix sockets.
     *
     * @throws IOException on error
     */
    @Override
    public SocketChannel connectImpl() throws IOException {
        if (getAddress().isListeningSocket()) {
            if (serverSocket == null || !serverSocket.isOpen()) {
                serverSocket = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                serverSocket.bind(unixSocketAddress);
            }
            socket = serverSocket.accept();
        } else {
            socket = SocketChannel.open(unixSocketAddress);
        }

        socket.configureBlocking(true);

        // TODO: No longer needed? See jdk.net.ExtendedSocketOptions.SO_PEERCRED (line 198)
        // MacOS and FreeBSD don't support SO_PASSCRED
//        if (!Util.isMacOs() && !FreeBSDHelper.isFreeBSD()) {
//            us.setOption(ExtendedSocketOptions.SO_PEERCRED, us.getOption(ExtendedSocketOptions.SO_PEERCRED));
//        }

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

    @Override
    public boolean isAbstractAllowed() {
        return false;
    }

    @Override
    public int getUid(SocketChannel _sock) throws IOException {
        return NativeUnixSocketHelper.getUid(_sock);
    }
}
