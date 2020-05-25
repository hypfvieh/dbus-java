package org.freedesktop.dbus.connections.transports;

import java.io.IOException;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.FreeBSDHelper;
import org.freedesktop.dbus.connections.SASL;

import com.github.hypfvieh.util.SystemUtil;

import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import jnr.unixsocket.UnixSocketOptions;

/**
 * Transport type representing a transport connection to a unix socket.
 * 
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public class UnixSocketTransport extends AbstractTransport {
    private final UnixSocketAddress unixSocketAddress;
    private UnixServerSocketChannel unixServerSocket;

    UnixSocketTransport(BusAddress _address) throws IOException {
        super(_address);

        if (_address.isAbstract()) {
            unixSocketAddress = new UnixSocketAddress("\0" + _address.getAbstract());
        } else if (_address.hasPath()) {
            unixSocketAddress = new UnixSocketAddress(_address.getPath());
        } else {
            throw new IOException("Unix socket url has to specify 'path' or 'abstract'");
        }

        setSaslAuthMode(SASL.AUTH_EXTERNAL);
    }

    @Override
    boolean hasFileDescriptorSupport() {
        return true; // file descriptor passing allowed when using UNIX_SOCK
    }

    /**
     * Establish a connection to DBus using unix sockets.
     * 
     * @throws IOException on error
     */
    @Override
    void connect() throws IOException {
        UnixSocketChannel us;
        if (getAddress().isListeningSocket()) {
            unixServerSocket = UnixServerSocketChannel.open();

            unixServerSocket.socket().bind(unixSocketAddress);
            us = unixServerSocket.accept();
        } else {
            us = UnixSocketChannel.open(unixSocketAddress);
        }

        us.configureBlocking(true);

        // MacOS and FreeBSD don't support SO_PASSCRED
        if (!SystemUtil.isMacOs() && !FreeBSDHelper.isFreeBSD()) {
            us.setOption(UnixSocketOptions.SO_PASSCRED, true);
        }

        authenticate(us.socket().getOutputStream(), us.socket().getInputStream(), us.socket());

        setInputOutput(us.socket());
    }

    @Override
    public void close() throws IOException {
        getLogger().debug("Disconnecting Transport");

        if (unixServerSocket != null && unixServerSocket.isOpen()) {
            unixServerSocket.close();
        }

        super.close();
    }
}
