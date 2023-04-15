package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.config.TransportConfigBuilder;
import org.freedesktop.dbus.exceptions.*;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builder to create transports of different types.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-17
 */
public final class TransportBuilder {

    private static final Logger                          LOGGER      = LoggerFactory.getLogger(TransportBuilder.class);
    private static final Map<String, ITransportProvider> PROVIDERS   = getTransportProvider();

    private TransportConfigBuilder<TransportConfigBuilder<?, TransportBuilder>, TransportBuilder> transportConfigBuilder;

    private TransportBuilder(TransportConfig _config) throws DBusException {
        transportConfigBuilder = new TransportConfigBuilder<>(() -> this);
        if (_config != null) {
            transportConfigBuilder.withConfig(_config);
        }
    }

    static Map<String, ITransportProvider> getTransportProvider() {
        Map<String, ITransportProvider> providers = new ConcurrentHashMap<>();
        try {
            ServiceLoader<ITransportProvider> spiLoader = ServiceLoader.load(ITransportProvider.class);
            for (ITransportProvider provider : spiLoader) {
                String providerBusType = provider.getSupportedBusType();
                if (providerBusType == null) { // invalid transport, ignore
                    LOGGER.warn("Transport {} is invalid: No bustype configured", provider.getClass());
                    continue;
                }
                providerBusType = providerBusType.toUpperCase(Locale.US);

                LOGGER.debug("Found provider '{}' named '{}' providing bustype '{}'", provider.getClass().getSimpleName(), provider.getTransportName(), providerBusType);

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
            LOGGER.error("Could not initialize service provider.", _ex);
        }
        return providers;
    }

    /**
     * Creates a new {@link TransportBuilder} instance with the given address.
     *
     * @param _address address, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     *
     */
    public static TransportBuilder create(String _address) throws DBusException {
        TransportConfig cfg = new TransportConfig();
        cfg.setBusAddress(BusAddress.of(_address));
        return new TransportBuilder(cfg);
    }

    /**
     * Creates a new {@link TransportBuilder} instance using the given configuration.
     *
     * @param _config config, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create(TransportConfig _config) throws DBusException {
        return new TransportBuilder(_config);
    }

    /**
     * Creates a new {@link TransportBuilder} instance using a empty transport configuration.
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create() throws DBusException {
        return new TransportBuilder(null);
    }

    /**
     * Creates a new {@link TransportBuilder} instance with the given address.
     *
     * @param _address address, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create(BusAddress _address) throws DBusException {
        Objects.requireNonNull(_address, "BusAddress required");
        return new TransportBuilder(new TransportConfig(_address));
    }

    /**
     * Creates a new {@link TransportBuilder} with a dynamically created address.
     *
     * @param _transportType type of session (e.g. UNIX or TCP)
     *
     * @return {@link TransportBuilder}
     *
     * @throws DBusException when invalid/unknown/unsupported transport type given
     */
    public static TransportBuilder createWithDynamicSession(String _transportType) throws DBusException {
        String dynSession = createDynamicSession(_transportType, false);
        if (dynSession == null) {
            throw new DBusException("Could not create dynamic session for transport type '" + _transportType + "'");
        }
        return create(dynSession);
    }

    /**
     * Returns the configuration builder to configure the transport.
     * @return TransportConfigBuilder
     */
    public TransportConfigBuilder<TransportConfigBuilder<?, TransportBuilder>, TransportBuilder> configure() {
        return transportConfigBuilder;
    }

    /**
     * Create the transport with the previously provided configuration.
     *
     * @return {@link AbstractTransport} instance
     *
     * @throws DBusException when creating transport fails
     * @throws IOException when autoconnect is true and connection to DBus failed
     */
    public AbstractTransport build() throws DBusException, IOException {
        BusAddress myBusAddress = getAddress();
        TransportConfig config = transportConfigBuilder.build();
        if (myBusAddress == null) {
            throw new DBusException("Transport requires a BusAddress, use withBusAddress() to configure before building");
        }

        AbstractTransport transport = null;
        ITransportProvider provider = PROVIDERS.get(config.getBusAddress().getBusType());
        if (provider == null) {
            throw new DBusException("No transport provider found for bustype " + config.getBusAddress().getBusType());
        } else {
            LOGGER.info("Using transport {} for address {}", provider.getTransportName(), config.getBusAddress());
        }

        try {
            transport = provider.createTransport(myBusAddress, config);
            Objects.requireNonNull(transport, "Transport required"); // in case the factory returns null, we cannot continue

            if (config.getSaslConfig().getAuthMode() > 0) {
                transport.getSaslConfig().setAuthMode(config.getSaslConfig().getAuthMode());
            }
        } catch (TransportConfigurationException _ex) {
            LOGGER.error("Could not initialize transport", _ex);
        }

        if (transport == null) {
            throw new DBusException("Unknown address type " + myBusAddress.getType() + " or no transport provider found for bus type " + myBusAddress.getBusType());
        }

        if (myBusAddress.isListeningSocket() && myBusAddress instanceof IFileBasedBusAddress) {
            ((IFileBasedBusAddress) myBusAddress).updatePermissions(config.getFileOwner(), config.getFileGroup(), config.getFileUnixPermissions());
        }

        transport.setPreConnectCallback(config.getPreConnectCallback());

        if (config.isAutoConnect()) {
            if (config.isListening()) {
                transport.listen();
            } else {
                transport.connect();
            }
        }
        return transport;
    }

    /**
     * The currently configured BusAddress.
     *
     * @return {@link BusAddress}
     */
    public BusAddress getAddress() {
        return configure().getBusAddress();
    }

    /**
     * Returns a {@link List} of all bustypes supported in the current runtime.
     *
     * @return {@link List}, maybe empty
     */
    public static List<String> getRegisteredBusTypes() {
        return new ArrayList<>(PROVIDERS.keySet());
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
        ITransportProvider provider = PROVIDERS.get(_busType.toUpperCase(Locale.US));
        if (provider != null) {
            return provider.createDynamicSessionAddress(_listeningAddress);
        }
        return null;
    }

    /**
     * Represents supported SASL authentication modes.
     *
     * @author hypfvieh
     * @since v4.0.0 - 2021-09-17
     */
    public enum SaslAuthMode {
        /** No authentication (allow everyone). */
        AUTH_ANONYMOUS(SASL.AUTH_ANON),
        /** Authentication using SHA Cookie. */
        AUTH_COOKIE(SASL.AUTH_SHA),
        /** External authentication (e.g. by user ID). */
        AUTH_EXTERNAL(SASL.AUTH_EXTERNAL);

        private final int authMode;

        SaslAuthMode(int _authMode) {
            authMode = _authMode;
        }

        public int getAuthMode() {
            return authMode;
        }

    }
}
