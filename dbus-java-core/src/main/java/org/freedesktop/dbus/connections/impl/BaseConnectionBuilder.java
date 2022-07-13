package org.freedesktop.dbus.connections.impl;

import java.nio.ByteOrder;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.ReceivingService;
import org.freedesktop.dbus.connections.ReceivingService.ReceivingServiceConfig;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.Message.Endian;
import org.freedesktop.dbus.utils.Util;

/**
 * Base class for connection builders containing commonly used options.
 *
 * @author hypfvieh
 * @since 4.1.1 - 2022-07-13
 *
 * @param <R> concrete type of connection builder
 */
public abstract class BaseConnectionBuilder<R extends BaseConnectionBuilder<?>> {

    private final Class<R>      returnType;

    private final String        address;

    private boolean             weakReference              = false;
    private byte                endianess                  = getSystemEndianness();
    private int                 timeout                    = AbstractConnection.TCP_CONNECT_TIMEOUT;

    private IDisconnectCallback disconnectCallback;

    private int                 signalThreadCount          = 1;
    private int                 errorThreadCount           = 1;
    private int                 methodCallThreadCount      = 4;
    private int                 methodReturnThreadCount    = 1;

    private int                 signalThreadPriority       = Thread.NORM_PRIORITY;
    private int                 errorThreadPriority        = Thread.NORM_PRIORITY;
    private int                 methodCallThreadPriority   = Thread.NORM_PRIORITY;
    private int                 methodReturnThreadPriority = Thread.NORM_PRIORITY;

    protected BaseConnectionBuilder(Class<R> _returnType, String _address) {
        returnType = _returnType;
        address = _address;
    }

    /**
     * Return ourselves.
     * @return concrete version of this
     */
    R self() {
        return returnType.cast(this);
    }

    /**
     * Creates the configuration for to use for {@link ReceivingService}.
     *
     * @return config
     */
    protected ReceivingServiceConfig buildThreadConfig() {
        return new ReceivingService.ReceivingServiceConfig(
                signalThreadCount, errorThreadCount, methodCallThreadCount, methodReturnThreadCount,
                signalThreadPriority, errorThreadPriority, methodCallThreadPriority, methodReturnThreadPriority
                );
    }

    protected boolean isWeakReference() {
        return weakReference;
    }

    protected byte getEndianess() {
        return endianess;
    }

    protected int getTimeout() {
        return timeout;
    }

    protected IDisconnectCallback getDisconnectCallback() {
        return disconnectCallback;
    }

    protected String getAddress() {
        return address;
    }

    /**
     * Set the size of the thread-pool used to handle signals from the bus.
     * Caution: Using thread-pool size &gt; 1 may cause signals to be handled out-of-order
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public R withSignalThreadCount(int _threads) {
        signalThreadCount = Math.max(1, _threads);
        return self();
    }

    /**
     * Set the size of the thread-pool used to handle error messages received on the bus.
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public R withErrorHandlerThreadCount(int _threads) {
        errorThreadCount = Math.max(1, _threads);
        return self();
    }

    /**
     * Set the size of the thread-pool used to handle methods calls previously sent to the bus.
     * The thread pool size has to be &gt; 1 to handle recursive calls.
     * <p>
     * Default: 4
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public R withMethodCallThreadCount(int _threads) {
        methodCallThreadCount = Math.max(1, _threads);
        return self();
    }

    /**
     * Set the size of the thread-pool used to handle method return values received on the bus.
     * <p>
     * Default: 1
     *
     * @param _threads int &gt;= 1
     * @return this
     */
    public R withMethodReturnThreadCount(int _threads) {
        methodReturnThreadCount = Math.max(1, _threads);
        return self();
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt;{@value Thread#MIN_PRIORITY} && &gt; {@value Thread#MAX_PRIORITY})
     *
     * @since 4.1.1 - 2022-07-13
     */
    public R withSignalThreadPriority(int _priority) {
        signalThreadPriority = Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        return self();
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt;{@value Thread#MIN_PRIORITY} && &gt; {@value Thread#MAX_PRIORITY})
     *
     * @since 4.1.1 - 2022-07-13
     */
    public R withErrorThreadPriority(int _priority) {
        errorThreadPriority = Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        return self();
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt;{@value Thread#MIN_PRIORITY} && &gt; {@value Thread#MAX_PRIORITY})
     *
     * @since 4.1.1 - 2022-07-13
     */
    public R withMethedCallThreadPriority(int _priority) {
        methodCallThreadPriority = Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        return self();
    }

    /**
     * Sets the thread priority of the created signal thread(s).
     * <p>
     * Default: {@link Thread#NORM_PRIORITY} ({@value Thread#NORM_PRIORITY});
     *
     * @param _priority int &gt;={@value Thread#MIN_PRIORITY} and &lt;= {@value Thread#MAX_PRIORITY}
     * @return this
     *
     * @throws IllegalArgumentException when value is out ouf range (value &lt;{@value Thread#MIN_PRIORITY} && &gt; {@value Thread#MAX_PRIORITY})
     *
     * @since 4.1.1 - 2022-07-13
     */
    public R withMethodReturnThreadPriority(int _priority) {
        methodReturnThreadPriority = Util.checkIntInRange(_priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        return self();
    }

    /**
     * Set the endianess for the connection
     * Default is based on system endianess.
     *
     * @param _endianess {@link Endian#BIG} or {@value Endian#LITTLE}
     * @return this
     */
    public R withEndianess(byte _endianess) {
        if (_endianess == Endian.BIG || _endianess == Endian.LITTLE) {
            endianess = _endianess;
        }
        return self();
    }

    /**
     * Set the timeout for the connection (used for TCP connections only). Default is
     * {@value AbstractConnection#TCP_CONNECT_TIMEOUT}.
     *
     * @param _timeout timeout
     * @return this
     */
    public R withTimeout(int _timeout) {
        timeout = _timeout;
        return self();
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
