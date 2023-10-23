package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.*;
import org.freedesktop.dbus.connections.base.ReceivingService;
import org.freedesktop.dbus.connections.config.*;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;

import java.nio.ByteOrder;

/**
 * Base class for connection builders containing commonly used options.
 *
 * @author hypfvieh
 * @since 4.2.0 - 2022-07-13
 *
 * @param <R> concrete type of connection builder
 */
public abstract class BaseConnectionBuilder<R extends BaseConnectionBuilder<R, C>, C extends AbstractConnection> {

    private final Class<R>                         returnType;

    private boolean                                weakReference = false;

    private IDisconnectCallback                    disconnectCallback;

    private final ReceivingServiceConfigBuilder<R> rsConfigBuilder;

    private final TransportConfigBuilder<?, R>     transportConfigBuilder;

    protected BaseConnectionBuilder(Class<R> _returnType, BusAddress _address) {
        returnType = _returnType;
        rsConfigBuilder = new ReceivingServiceConfigBuilder<>(() -> self());
        transportConfigBuilder = new TransportConfigBuilder<>(() -> self());
        transportConfigBuilder.withBusAddress(_address);
    }

    /**
     * Return ourselves.
     * @return concrete version of this
     */
    R self() {
        return returnType.cast(this);
    }

    /**
     * Creates the configuration to use for {@link ReceivingService}.
     *
     * @return config
     */
    protected ReceivingServiceConfig buildThreadConfig() {
        return rsConfigBuilder.build();
    }

    /**
     * Creates the configuration to use for {@link TransportBuilder}.
     *
     * @return config
     */
    protected TransportConfig buildTransportConfig() {
        return transportConfigBuilder.build();
    }

    protected boolean isWeakReference() {
        return weakReference;
    }

    protected IDisconnectCallback getDisconnectCallback() {
        return disconnectCallback;
    }

    /**
     * Returns the builder to configure the receiving thread pools.
     * @return builder
     */
    public ReceivingServiceConfigBuilder<R> receivingThreadConfig() {
        return rsConfigBuilder;
    }

    /**
     * Returns the builder to configure the used transport.
     * @return builder
     */
    public TransportConfigBuilder<?, R> transportConfig() {
        return transportConfigBuilder;
    }

    /**
     * Enable/Disable weak references on connection.
     * Default is false.
     *
     * @param _weakRef true to enable
     * @return this
     */
    public R withWeakReferences(boolean _weakRef) {
        weakReference = _weakRef;
        return self();
    }

    /**
     * Set the given disconnect callback to the created connection.
     *
     * @param _disconnectCallback callback
     * @return this
     */
    public R withDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        disconnectCallback = _disconnectCallback;
        return self();
    }

    public abstract C build() throws DBusException;

    /**
     * Get the default system endianness.
     *
     * @return LITTLE or BIG
     */
    public static byte getSystemEndianness() {
       return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                ? Message.Endian.BIG
                : Message.Endian.LITTLE;
    }
}
