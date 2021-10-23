package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.util.List;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * Factory to create connection to DBus using unix socket or TCP.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 *
 * @deprecated use {@link TransportBuilder} instead
 */
@Deprecated(forRemoval = true, since = "4.0.0")
public final class TransportFactory {

    private TransportFactory() {

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
        try {
            return TransportBuilder.create(_address).withTimeout(_timeout).withAutoConnect(_connect).build();
        } catch (DBusException _ex) {
            throw new IOException(_ex);
        }
    }

    /**
     * Creates a new dynamic bus address for the given bus type.
     *
     * @param _busType bus type (e.g. UNIX or TCP), never null
     * @param _listeningAddress true if a listening (server) address should be created, false otherwise
     *
     * @return String containing BusAddress or null
     */
    public static String createDynamicSession(String _busType, boolean _listeningAddress) {
        return TransportBuilder.createDynamicSession(_busType, _listeningAddress);
    }

    /**
     * Returns a {@link List} of all bustypes supported in the current runtime.
     *
     * @return {@link List}, maybe empty
     */
    public static List<String> getRegisteredBusTypes() {
        return TransportBuilder.getRegisteredBusTypes();
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


}
