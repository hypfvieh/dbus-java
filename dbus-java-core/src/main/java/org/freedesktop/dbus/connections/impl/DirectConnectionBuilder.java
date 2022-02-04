package org.freedesktop.dbus.connections.impl;

import java.nio.ByteOrder;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.AddressBuilder;

/**
 * Builder to create a new DirectConnection.
 *
 * @author hypfvieh
 * @version 4.1.0 - 2022-02-04
 */
public class DirectConnectionBuilder {

    private final String        address;

    private boolean             weakReference = false;
    private byte                endianess     = getSystemEndianness();
    private int                 timeout       = AbstractConnection.TCP_CONNECT_TIMEOUT;
    private IDisconnectCallback disconnectCallback;

    private DirectConnectionBuilder(String _address) {
        address = _address;
    }

    /**
     * Create new default connection to the DBus system bus.
     * 
     * @return {@link DirectConnectionBuilder}
     */
    public static DirectConnectionBuilder forSystemBus() {
        String address = AddressBuilder.getSystemConnection();
        address = tcpFallback(address);
        return new DirectConnectionBuilder(address);
    }

    /**
     * Create a new default connection connecting to the DBus session bus.
     * 
     * @return {@link DirectConnectionBuilder}
     */
    public static DirectConnectionBuilder forSessionBus() {
        String address = AddressBuilder.getSessionConnection(null);
        address = tcpFallback(address);
        DirectConnectionBuilder instance = new DirectConnectionBuilder(address);
        return instance;
    }

    /**
     * Create a default connection to DBus using the given bus type.
     * 
     * @param _type bus type
     * 
     * @return this
     */
    public static DirectConnectionBuilder forType(DBusBusType _type) {
        if (_type == DBusBusType.SESSION) {
            return forSessionBus();
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
    public static DirectConnectionBuilder forAddress(String _address) {
        DirectConnectionBuilder instance = new DirectConnectionBuilder(_address);
        return instance;
    }

    /**
     * Set the timeout for the connection (used for TCP connections only). Default is
     * {@value AbstractConnection.TCP_CONNECT_TIMEOUT}.
     * 
     * @param _timeout timeout
     * @return this
     */
    public DirectConnectionBuilder withTimeout(int _timeout) {
        timeout = _timeout;
        return this;
    }
    
    /**
     * Set the endianess for the connection 
     * Default is based on system endianess.
     * 
     * @param _endianess {@value Message.Endian.BIG} or {@value Message.Endian.LITTLE}
     * @return this
     */
    public DirectConnectionBuilder withEndianess(byte _endianess) {
        if (_endianess == Message.Endian.BIG || _endianess == Message.Endian.LITTLE) {
            endianess = _endianess;
        }
        return this;
    }

    /**
     * Set the given disconnect callback to the created connection.
     * 
     * @param _disconnectCallback callback
     * @return this
     */
    public DirectConnectionBuilder withDisconnectCallback(IDisconnectCallback _disconnectCallback) {
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
    public DirectConnectionBuilder withWeakReferences(boolean _weakRef) {
        weakReference = _weakRef;
        return this;
    }

    /**
     * Create the new {@link DBusConnection}.
     * 
     * @return {@link DBusConnection}
     * @throws DBusException when DBusConnection could not be opened
     */
    public DirectConnection build() throws DBusException {
        DirectConnection c = new DirectConnection(address, timeout);
        c.setDisconnectCallback(disconnectCallback);
        c.setWeakReferences(weakReference);
        DirectConnection.setEndianness(endianess);
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

    /**
     * Get the default system endianness.
     *
     * @return LITTLE or BIG
     */
    public static byte getSystemEndianness() {
       return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ?
                Message.Endian.BIG
                : Message.Endian.LITTLE;
    }
}
