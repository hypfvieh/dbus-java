package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;

/**
 * Transport type representing a transport connection to TCP.
 * 
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public class TcpTransport extends AbstractTransport {

    private Socket socket;
    private final int        timeout;

    
    TcpTransport(BusAddress _address, int _timeout) {
        super(_address);
        timeout = _timeout;
        setSaslAuthMode(SASL.AUTH_SHA);
    }

    @Override
    boolean hasFileDescriptorSupport() {
        return false; // file descriptor passing not possible on TCP connections
    }

    /**
     * Connect to DBus using TCP.
     * @throws IOException on error
     */
    void connect() throws IOException {
        
        if (getAddress().isListeningSocket()) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.bind(new InetSocketAddress(getAddress().getHost(), getAddress().getPort()));
                socket = ss.accept();
            }
        } else {
            socket = new Socket();
            getLogger().trace("Setting timeout to {} on Socket", timeout);
            socket.connect(new InetSocketAddress(getAddress().getHost(), getAddress().getPort()), timeout);
        }
        
        setInputOutput(socket);

        authenticate(socket.getOutputStream(), socket.getInputStream(), socket);
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        super.close();
    }
}

