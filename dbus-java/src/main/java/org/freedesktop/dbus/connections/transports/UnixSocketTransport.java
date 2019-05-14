package org.freedesktop.dbus.connections.transports;

import java.io.IOException;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;

import com.github.hypfvieh.util.SystemUtil;

import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import jnr.unixsocket.UnixSocketOptions;

/**
 * Transport type representing a transport connection to a unix socket.
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public class UnixSocketTransport extends AbstractTransport {
    private final UnixSocketAddress unixSocketAddress;
    private UnixServerSocketChannel unixServerSocket;

    UnixSocketTransport(BusAddress _address, int _timeout) throws IOException {
        super(_address, _timeout); 
        
        if (_address.isAbstract()) {
            unixSocketAddress = new UnixSocketAddress("\0" + _address.getAbstract());
        } else if (_address.hasPath()) {
            unixSocketAddress = new UnixSocketAddress(_address.getPath());
        } else {
            throw new IOException("Unix socket url has to specify 'path' or 'abstract'");
        }
        
        setSaslAuthMode(SASL.AUTH_EXTERNAL);
    }

    /**
     * Establish a connection to DBus using unix sockets.
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
        
        // MacOS doesn't support SO_PASSCRED
        if (!SystemUtil.isMacOs()) {
            us.setOption(UnixSocketOptions.SO_PASSCRED, true);
        }

        getLogger().trace("Setting timeout to {} on unix socket", getTimeout());
        
        us.configureBlocking(true);
        
        if (getTimeout() == 1) {
            us.socket().setSoTimeout(0);
        } else {
            us.socket().setSoTimeout(getTimeout());
        }
        
        setOutputWriter(us.socket().getOutputStream());
        setInputReader(us.socket().getInputStream());
        
        authenticate(us.socket().getOutputStream(), us.socket().getInputStream(), us.socket());
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
