package org.freedesktop.dbus.connections.impl;

import static org.freedesktop.dbus.utils.AddressBuilder.getDbusMachineId;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.utils.AddressBuilder;

/**
 * Builder to create a new DBusConnection.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-04
 */
public class DBusConnectionBuilder {

    private final String                                       address;

    private final String                                       machineId;
    private boolean                                            registerSelf = true;
    private boolean                                            shared       = true;
    private boolean                                            weakReference = false;
    private int                                                timeout      = AbstractConnection.TCP_CONNECT_TIMEOUT;
    private IDisconnectCallback                                disconnectCallback;

    private DBusConnectionBuilder(String _address, String _machineId) {
        address = _address;
        machineId = _machineId;
    }

    /**
     * Create a new default connection connecting to DBus session bus but use an alternative input for the machineID.
     * 
     * @param _machineIdFileLocation file with machine ID
     * 
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSessionBus(String _machineIdFileLocation) {
        String address = AddressBuilder.getSessionConnection(_machineIdFileLocation);
        address = tcpFallback(address);
        DBusConnectionBuilder instance = new DBusConnectionBuilder(address, getDbusMachineId(_machineIdFileLocation));
        return instance;
    }

    /**
     * Create new default connection to the DBus system bus.
     * 
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSystemBus() {
        String address = AddressBuilder.getSystemConnection();
        address = tcpFallback(address);
        return new DBusConnectionBuilder(address, getDbusMachineId(null));
    }

    /**
     * Create a new default connection connecting to the DBus session bus.
     * 
     * @return {@link DBusConnectionBuilder}
     */
    public static DBusConnectionBuilder forSessionBus() {
        return forSessionBus(null);
    }

    /**
     * Create a default connection to DBus using the given bus type.
     * 
     * @param _type bus type
     * 
     * @return this
     */
    public static DBusConnectionBuilder forType(DBusBusType _type) {
        return forType(_type, null);
    }

    /**
     * Create a default connection to DBus using the given bus type and machineIdFile.
     * 
     * @param _type bus type
     * @param _machineIdFile machineId file
     * 
     * @return this
     */
    public static DBusConnectionBuilder forType(DBusBusType _type, String _machineIdFile) {
        if (_type == DBusBusType.SESSION) {
            return forSessionBus(_machineIdFile);
        } else if (_type == DBusBusType.SYSTEM) {
            return forSystemBus();
        }

        throw new IllegalArgumentException("Unknown bus type: " + _type);
    }

    /**
     * Use the given address to create the connection (e.g. used for remote TCP connected DBus daemons).
     * 
     * @param _address address to use
     * @return this
     */
    public static DBusConnectionBuilder forAddress(String _address) {
        DBusConnectionBuilder instance = new DBusConnectionBuilder(_address, getDbusMachineId(null));
        return instance;
    }

    /**
     * Register the new connection on DBus using 'hello' message. Default is true.
     * 
     * @param _register boolean
     * @return this
     */
    public DBusConnectionBuilder withRegisterSelf(boolean _register) {
        registerSelf = _register;
        return this;
    }

    /**
     * Use this connection as shared connection. Shared connection means that the same connection is used multiple times
     * if the connection parameter did not change. Default is true.
     * 
     * @param _shared boolean
     * @return this
     */
    public DBusConnectionBuilder withShared(boolean _shared) {
        shared = _shared;
        return this;
    }

    /**
     * Set the timeout for the connection (used for TCP connections only). Default is
     * {@value AbstractConnection.TCP_CONNECT_TIMEOUT}.
     * 
     * @param _timeout timeout
     * @return this
     */
    public DBusConnectionBuilder withTimeout(int _timeout) {
        timeout = _timeout;
        return this;
    }

    /**
     * Set the given disconnect callback to the created connection.
     * 
     * @param _disconnectCallback callback
     * @return this
     */
    public DBusConnectionBuilder withDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        disconnectCallback = _disconnectCallback;
        return this;
    }

    /**
     * Enable/Disable weak references on connection.
     * Default is false.
     * 
     * @param _weakRef true to enable
     * @return this
     */
    public DBusConnectionBuilder withWeakReferences(boolean _weakRef) {
        weakReference = _weakRef;
        return this;
    }

    /**
     * Create the new {@link DBusConnection}.
     * 
     * @return {@link DBusConnection}
     * @throws DBusException when DBusConnection could not be opened
     */
    public DBusConnection build() throws DBusException {
        DBusConnection c;
        if (shared) {
            synchronized (DBusConnection.CONNECTIONS) {
                c = DBusConnection.CONNECTIONS.get(address);
                if (c != null) {
                    c.concurrentConnections.incrementAndGet();
                    return c; // this connection already exists, do not change anything
                } else {
                    c = new DBusConnection(address, shared, machineId, timeout);
                    DBusConnection.CONNECTIONS.put(address, c);
                }
            }
        } else {
            c = new DBusConnection(address, shared, machineId, timeout);
        }
        
        c.setDisconnectCallback(disconnectCallback);
        c.setWeakReferences(weakReference);
        c.connect(registerSelf);
        return c;
    }

    /**
     * Helper to use TCP fallback address when no UNIX address is found or no UNIX transport found.
     * 
     * @param _address address which would be used if no fallback is used
     * @return input address or fallback address
     */
    private static String tcpFallback(String _address) {
        if (!TransportBuilder.getRegisteredBusTypes().contains("UNIX") // no unix transport
                && TransportBuilder.getRegisteredBusTypes().contains("TCP") // but tcp transport
                && (_address == null || _address.startsWith("unix"))) { // no address or unix socket address

            // no UNIX transport available, or lookup did not return anything useful
            _address = System.getProperty(AbstractConnection.TCP_ADDRESS_PROPERTY);
        }
        return _address;
    }

}
