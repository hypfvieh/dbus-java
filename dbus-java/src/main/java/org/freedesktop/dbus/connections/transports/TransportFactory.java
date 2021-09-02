package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.util.Random;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.BusAddress.AddressBusTypes;
import org.freedesktop.dbus.utils.Hexdump;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

/**
 * Factory to create connection to DBus using unix socket or TCP.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public final class TransportFactory {

    /**
     * System property to disable the usage of jnr-unixsockets.
     */
    public static final String DBUS_JAVA_DISABLE_JNR_UNIXSOCKET = "dbus.java.disable.jnr-unixsocket";
    public static final boolean JNR_UNIXSOCKET_AVAILABLE = checkJnrUnixSocketAvailable();

    private TransportFactory() {

    }

    private static boolean checkJnrUnixSocketAvailable() {
        if (Boolean.getBoolean(DBUS_JAVA_DISABLE_JNR_UNIXSOCKET)) {
            return false;
        }

        try {
            Class.forName("jnr.unixsocket.UnixSocketAddress");
            return true;
        } catch (ClassNotFoundException _ex) {
            return false;
        }
    }

    /**
     * Creates a new transport encapsulating connection to a UNIX or TCP socket.
     *
     * @param _address Address parameter
     * @param _timeout timeout in milliseconds
     * @param _connect open the connection before return
     *
     * @return {@link AbstractTransport}
     * @throws IOException when transport could not be created
     */
    public static AbstractTransport createTransport(BusAddress _address, int _timeout, boolean _connect) throws IOException {
        LoggerFactory.getLogger(TransportFactory.class).debug("Connecting to {}", _address);

        AbstractTransport transport;

        if (_address.getBusType() == AddressBusTypes.UNIX) {
            transport = getUnixTransport(_address);
        } else if (_address.getBusType() == AddressBusTypes.TCP) {
            transport = new TcpTransport(_address, _timeout);
        } else {
            throw new IOException("Unknown address type " + _address.getType());
        }

        if (_connect) {
            transport.connect();
        }
        return transport;
    }

    private static AbstractTransport getUnixTransport(BusAddress _address) throws IOException {
        if (Util.getJavaVersion() >= 16 && !JNR_UNIXSOCKET_AVAILABLE) {
            return new NativeUnixSocketTransport(_address);
        }
        return new UnixSocketTransport(_address);
    }

    /**
     * Creates a new transport encapsulating connection to a UNIX or TCP socket.
     *
     * @param _address Address parameter
     * @return {@link AbstractTransport}
     * @throws IOException when transport could not be created
     */
    public static AbstractTransport createTransport(BusAddress _address) throws IOException {
        return createTransport(_address, 10000, true);
    }

    /**
     * Creates a new transport encapsulating connection to a UNIX or TCP socket.
     *
     * @param _address Address parameter
     * @param _timeout timeout in milliseconds
     *
     * @return {@link AbstractTransport}
     * @throws IOException when transport could not be created
     */
    public static AbstractTransport createTransport(BusAddress _address, int _timeout) throws IOException {
        return createTransport(_address, _timeout, true);
    }

    public static String genGUID() {
        Random r = new Random();
        byte[] buf = new byte[16];
        r.nextBytes(buf);
        String guid = Hexdump.toHex(buf);
        return guid.replaceAll(" ", "");
    }
}
