package org.freedesktop.dbus.connections.impl;

import java.nio.ByteOrder;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;

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
        DirectConnection c = new DirectConnection(timeout, address);
        c.setDisconnectCallback(disconnectCallback);
        c.setWeakReferences(weakReference);
        DirectConnection.setEndianness(endianess);
        return c;
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
