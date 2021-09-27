package org.freedesktop.dbus.connections.transports;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.exceptions.TransportRegistrationException;
import org.freedesktop.dbus.spi.transport.ITransportProvider;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder to create transports of different types.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-17
 */
public class TransportBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportBuilder.class);
    private static final Map<String, ITransportProvider> PROVIDERS = getTransportProvider();

    private String address;
    private BusAddress busAddress;

    private boolean listening;
    private int timeout = AbstractConnection.TCP_CONNECT_TIMEOUT;
    private boolean autoConnect = true;
    private SaslAuthMode authMode = null;

    /** user to set on socket file if this is a server transport (null to do nothing). */
    private String fileOwner;
    /** group to set on socket file if this is a server transport (null to do nothing). */
    private String fileGroup;
    /** unix file permissions to set on socket file if this is a server transport (ignored on Windows, does nothing if null) */
    private Set<PosixFilePermission> fileUnixPermissions;

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
                providerBusType = providerBusType.toUpperCase();

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

    private TransportBuilder(String _address) throws DBusException {
        if (_address == null || _address.isBlank()) {
            throw new IllegalArgumentException("BusAddress cannot be empty or null");
        }
        address = _address;
        updateAddress();
    }

    private TransportBuilder(BusAddress _address) throws DBusException {
        Objects.requireNonNull(_address, "Address required");
        address = _address.getRawAddress();
        if (_address.isListeningSocket()) {
            listening = true;
        }
        busAddress = _address;
    }

    /**
     * Creates a new {@link TransportBuilder} instance with the given address.
     *
     * @param _address address, never null
     *
     * @return new {@link TransportBuilder}
     * @throws DBusException if invalid address provided
     */
    public static TransportBuilder create(String _address) throws DBusException {
        return new TransportBuilder(_address);
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
        return new TransportBuilder(_address);
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
        return new TransportBuilder(dynSession);
    }

    /**
     * Set the connection timeout (usually only used for TCP based transports).
     * <p>
     * default: {@link AbstractConnection#TCP_CONNECT_TIMEOUT}
     *
     * @param _timeout timeout, if &lt; 0 default timeout of {@link AbstractConnection#TCP_CONNECT_TIMEOUT} will be used
     *
     * @return this
     */
    public TransportBuilder withTimeout(int _timeout) {
        timeout = _timeout < 0 ? AbstractConnection.TCP_CONNECT_TIMEOUT : _timeout;
        return this;
    }

    /**
     * Toggle the created transport to be a listening (server) or initiating (client) connection.
     * <p>
     * Default is a client connection.
     *
     * @param _listen true to create a listening transport (e.g. for server usage)
     * @return
     */
    public TransportBuilder isListening(boolean _listen) {
        listening = _listen;
        return this;
    }

    /**
     * Instantly connect to DBus when {@link #build()} is called.
     * <p>
     * default: true
     *
     * @param _connect boolean
     *
     * @return this
     */
    public TransportBuilder withAutoConnect(boolean _connect) {
        autoConnect = _connect;
        return this;
    }

    /**
     * Set a different SASL authentication mode.
     * <p>
     * Usually when a unixsocket based transport is used, {@link SaslAuthMode#AUTH_EXTERNAL} will be used.
     * For TCP based transport {@link SaslAuthMode#AUTH_COOKIE} will be used.
     * <p>
     *
     * @param _authMode authmode to use, if null is given, default mode will be used
     * @return this
     */
    public TransportBuilder withSaslAuthMode(SaslAuthMode _authMode) {
        authMode = _authMode;
        return this;
    }

    /**
     * The owner of the socket file if a unix socket is used and this is a server transport.
     * <p>
     * Default is the user of the running JVM process.<br><br>
     * <b>Please note:</b><br>
     * The running process user has to have suitable permissions to change the owner
     * of the file. Otherwise the file owner will not be changed!
     *
     * @param _user user to set, if null is given JVM process user is used
     *
     * @return this
     */
    public TransportBuilder withUnixSocketFileOwner(String _user) {
        fileOwner = _user;
        return this;
    }

    /**
     * The group of the socket file if a unix socket is used and this is a server transport.
     * <p>
     * Default is the group of the running JVM process.<br><br>
     * <b>Please note:</b><br>
     * The running process user has to have suitable permissions to change the group
     * of the file. Otherwise the file group will not be changed!
     *
     * @param _group group to set, if null is given JVM process group is used
     *
     * @return this
     */
    public TransportBuilder withUnixSocketFileGroup(String _group) {
        fileGroup = _group;
        return this;
    }

    /**
     * The permissions which will be set on socket file if a unix socket is used and this is a server transport.
     * <p>
     * This method does nothing when used on windows systems.
     * <b>Please note:</b><br>
     * The running process user has to have suitable permissions to change the permissions
     * of the file. Otherwise the file permissions will not be changed!
     *
     * @param _permissions permissions to set, if null is given default permissions will be used
     *
     * @return this
     */
    public TransportBuilder withUnixSocketFilePermissions(PosixFilePermission... _permissions) {
        if (Util.isWindows()) {
            return this;
        }

        if (_permissions == null || _permissions.length < 1) {
            return this;
        }
        fileUnixPermissions = new LinkedHashSet<>(Arrays.asList(_permissions));
        return this;
    }

    /**
     * Setup the unix socket file permissions.
     */
    private void setFilePermissions(Path _path) {
        UserPrincipalLookupService userPrincipalLookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        try {
            UserPrincipal userPrincipal = userPrincipalLookupService.lookupPrincipalByName(fileOwner);

            if (!Util.isBlank(fileOwner)) {
                try {
                    if (userPrincipal != null) {
                        Files.getFileAttributeView(_path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(userPrincipal);
                    }
                } catch (IOException _ex) {
                    LOGGER.error("Could not change owner of {} to {}", _path, fileOwner, _ex);
                }
            }

            if (!Util.isBlank(fileGroup)) {
                try {
                    GroupPrincipal groupPrincipal = userPrincipalLookupService.lookupPrincipalByGroupName(fileGroup);
                    if (groupPrincipal != null) {
                        Files.getFileAttributeView(_path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(groupPrincipal);
                    }
                } catch (IOException _ex) {
                    LOGGER.error("Could not change group of {} to {}", _path, fileGroup, _ex);
                }
            }

            if (!Util.isWindows() && fileUnixPermissions != null) {
                try {
                    Files.setPosixFilePermissions(_path, fileUnixPermissions);
                } catch (Exception _ex) {
                    LOGGER.error("Could not set file permissions of {} to {}", _path, fileUnixPermissions, _ex);
                }
            }

        } catch (IOException _ex) {
            LOGGER.error("Not setting any permission/user/group: Error getting default principal lookup service", _ex);
        }

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
        updateAddress();

        BusAddress busAddress = getAddress();

        AbstractTransport transport = null;
        ITransportProvider provider = PROVIDERS.get(busAddress.getBusType());
        if (provider == null) {
            throw new DBusException("No transport provider found for bustype " + busAddress.getBusType());
        }

        try {
            transport = provider.createTransport(busAddress, timeout);
            if (authMode != null) {
                transport.setSaslAuthMode(authMode.getAuthMode());
            }
        } catch (TransportConfigurationException _ex) {
            LOGGER.error("Could not initialize transport", _ex);
        }

        if (transport == null) {
            throw new DBusException("Unknown address type " + busAddress.getType() + " or no transport provider found for bus type " + busAddress.getBusType());
        }

        if (busAddress.isListeningSocket()) {
            setFilePermissions(new File(busAddress.getPath()).toPath());
        }

        if (autoConnect) {
            transport.connect();
        }
        return transport;
    }

    /**
     * The currently configured BusAddress.
     *
     * @return {@link BusAddress}
     */
    public BusAddress getAddress() {
        return busAddress;
    }

    private void updateAddress() throws DBusException {

        busAddress = new BusAddress(address);
        if (!busAddress.isListeningSocket() && listening) { // not a listening address, but should be one
            address += ",listen=true";
            busAddress = new BusAddress(address);
        } else if (busAddress.isListeningSocket() && !listening) { // listening address, but should not be one
            address = address.replace(",listen=true", "");
            busAddress = new BusAddress(address);
        }
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
        ITransportProvider provider = PROVIDERS.get(_busType.toUpperCase());
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
    public static enum SaslAuthMode {
        /** No authentication (allow everyone). */
        AUTH_ANONYMOUS(SASL.AUTH_ANON),
        /** Authentication using SHA Cookie. */
        AUTH_COOKIE(SASL.AUTH_SHA),
        /** External authentication (e.g. by user ID). */
        AUTH_EXTERNAL(SASL.AUTH_EXTERNAL);

        private final int authMode;

        private SaslAuthMode(int _authMode) {
            authMode = _authMode;
        }

        public int getAuthMode() {
            return authMode;
        }

    }
}
