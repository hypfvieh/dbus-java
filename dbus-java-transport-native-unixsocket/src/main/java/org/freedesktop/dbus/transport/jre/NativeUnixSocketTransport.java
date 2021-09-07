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
    private ServerSocketChannel unixServerSocket;

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
        SocketChannel us;
        if (getAddress().isListeningSocket()) {
            unixServerSocket = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            unixServerSocket.bind(unixSocketAddress);
            us = unixServerSocket.accept();
        } else {
            us = SocketChannel.open(unixSocketAddress);
        }

        us.configureBlocking(true);

        // TODO: No longer needed? See jdk.net.ExtendedSocketOptions.SO_PEERCRED (line 198)
        // MacOS and FreeBSD don't support SO_PASSCRED
//        if (!Util.isMacOs() && !FreeBSDHelper.isFreeBSD()) {
//            us.setOption(ExtendedSocketOptions.SO_PEERCRED, us.getOption(ExtendedSocketOptions.SO_PEERCRED));
//        }

        return us;
    }

    @Override
    public void close() throws IOException {
        getLogger().debug("Disconnecting Transport");

        if (unixServerSocket != null && unixServerSocket.isOpen()) {
            unixServerSocket.close();
        }

        super.close();
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
