package org.freedesktop.dbus.transport.jnr;

import jnr.posix.util.Platform;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import jnr.unixsocket.UnixSocketOptions;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.transports.AbstractUnixTransport;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.utils.Util;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Transport type representing a transport connection to a unix socket.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public class UnixSocketTransport extends AbstractUnixTransport {
    private final UnixSocketAddress unixSocketAddress;

    private UnixSocketChannel       socket;
    private UnixServerSocketChannel serverSocket;

    UnixSocketTransport(JnrUnixBusAddress _address, TransportConfig _config) throws TransportConfigurationException {
        super(_address, _config);

        if (_address.isAbstract()) {
            unixSocketAddress = new UnixSocketAddress("\0" + _address.getAbstract());
        } else if (_address.hasPath()) {
            unixSocketAddress = new UnixSocketAddress(_address.getPath());
        } else {
            throw new TransportConfigurationException("Unix socket url has to specify 'path' or 'abstract'");
        }

        getSaslConfig().setAuthMode(SASL.AUTH_EXTERNAL);
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
                serverSocket = UnixServerSocketChannel.open();
                serverSocket.configureBlocking(true);
                serverSocket.socket().bind(unixSocketAddress);
            }
            socket = serverSocket.accept();
        } else {
            socket = UnixSocketChannel.open(unixSocketAddress);
            socket.configureBlocking(true);
        }

        // MacOS and FreeBSD don't support SO_PASSCRED
        if (!Util.isMacOs() && !Platform.IS_FREEBSD) {
            socket.setOption(UnixSocketOptions.SO_PASSCRED, true);
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
        return true;
    }

    @Override
    public int getUid(SocketChannel _sock) throws IOException {
        return JnrUnixSocketHelper.getUid(_sock);
    }
}
