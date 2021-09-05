package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory to create connection to DBus using unix socket or TCP.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public final class TransportFactory {

    private static final TransportFactory INSTANCE = new TransportFactory();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<ITransportProvider> providers = new ArrayList<>();

    private TransportFactory() {
        try {
            ServiceLoader<ITransportProvider> spiLoader = ServiceLoader.load(ITransportProvider.class);
            for (ITransportProvider provider : spiLoader) {
                logger.debug("Found {} {}", ITransportProvider.class.getSimpleName(), provider.getTransportName());
                providers.add(provider);
            }
        } catch (ServiceConfigurationError _ex) {
            logger.error("Could not initialize service provider.", _ex);
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

        AbstractTransport transport = null;

        for (ITransportProvider provider : INSTANCE.providers) {
            try {
                transport = provider.createTransport(_address, _timeout);
                if (transport != null) {
                    break;
                }
            } catch (TransportConfigurationException _ex) {
                INSTANCE.logger.error("Could not initialize transport", _ex);
            }
        }

        if (transport == null) {
            throw new IOException("Unknown address type " + _address.getType() + " or no transport provider found for this address type");
        }

        if (_connect) {
            transport.connect();
        }
        return transport;
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
