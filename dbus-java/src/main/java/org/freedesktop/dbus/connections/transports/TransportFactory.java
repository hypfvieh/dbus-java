package org.freedesktop.dbus.connections.transports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.exceptions.TransportRegistrationException;
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
    private final Map<String, ITransportProvider> providers = new ConcurrentHashMap<>();

    private TransportFactory() {
        try {
            ServiceLoader<ITransportProvider> spiLoader = ServiceLoader.load(ITransportProvider.class);
            for (ITransportProvider provider : spiLoader) {
                String providerBusType = provider.getSupportedBusType();
                if (providerBusType == null) { // invalid transport, ignore
                    logger.warn("Transport {} is invalid: No bustype configured", provider.getClass());
                    continue;
                }
                providerBusType = providerBusType.toUpperCase();

                logger.debug("Found provider '{}' named '{}' providing bustype '{}'", provider.getClass().getSimpleName(), provider.getTransportName(), providerBusType);

                if (providers.containsKey(providerBusType)) {
                    throw new TransportRegistrationException("Found transport "
                            + providers.get(providerBusType).getClass().getName()
                            + " and "
                            + provider.getClass().getName() + " both providing transport for socket type "
                            + providerBusType + ", please only add one of them to classpath.");
                }
                providers.put(providerBusType, provider);
            }
            if (providers.isEmpty()) {
                throw new TransportRegistrationException("No dbus-java-transport found in classpath, please add a transport module");
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
        ITransportProvider provider = INSTANCE.providers.get(_address.getBusType());
        if (provider == null) {
            throw new IOException("No transport provider found for bustype " + _address.getBusType());
        }

        try {
            transport = provider.createTransport(_address, _timeout);
        } catch (TransportConfigurationException _ex) {
            INSTANCE.logger.error("Could not initialize transport", _ex);
        }

        if (transport == null) {
            throw new IOException("Unknown address type " + _address.getType() + " or no transport provider found for bus type " + _address.getBusType());
        }

        if (_connect) {
            transport.connect();
        }
        return transport;
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
        Objects.requireNonNull(_busType, "Bustype required");
        ITransportProvider provider = INSTANCE.providers.get(_busType.toUpperCase());
        if (provider != null) {
            return provider.createDynamicSessionAddress(_listeningAddress);
        }
        return null;
    }

    /**
     * Returns a {@link List} of all bustypes supported in the current runtime.
     * 
     * @return {@link List}, maybe empty
     */
    public static List<String> getRegisteredBusTypes() {
        return new ArrayList<>(INSTANCE.providers.keySet());
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
