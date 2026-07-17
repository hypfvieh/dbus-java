package org.freedesktop.dbus.connections.base;

import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.IDisconnectAction;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.ConnectionConfig;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.errors.UnknownProperty;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalDBusException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.messages.*;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.constants.Flags;
import org.freedesktop.dbus.utils.IThrowingConsumer;
import org.freedesktop.dbus.utils.IThrowingFunction;
import org.freedesktop.dbus.utils.NameableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class containing most parts required for a arbitrary connection.<br>
 * It is not intended to be used directly, therefore it is sealed.
 *
 * @since 5.0.0 - 2023-10-23
 * @author hypfvieh
 */
public abstract sealed class AbstractConnectionBase implements Closeable permits ConnectionMethodInvocation {

    private static final Map<Thread, DBusCallInfo> INFOMAP = new ConcurrentHashMap<>();

    /** Upper bound for {@link #pendingErrorQueue} to avoid unbounded growth when {@link #getError()} is never called. */
    private static final int                       MAX_PENDING_ERRORS = 1024;

    private final Logger                                                          logger;

    private final ObjectTree                                                      objectTree;

    private final Map<String, ExportedObject>                                     exportedObjects;
    private final Map<DBusInterface, RemoteObject>                                importedObjects;

    private final PendingCallbackManager                                          callbackManager;

    private final FallbackContainer                                               fallbackContainer;

    private final ExecutorService                                                 senderService;
    private final ReceivingService                                                receivingService;
    private final IncomingMessageThread                                           readerThread;

    private final Map<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> handledSignals;
    private final Map<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>>           genericHandledSignals;
    private final Map<Long, MethodCall>                                           pendingCalls;

    private final Queue<Error>                                                    pendingErrorQueue;
    private final AtomicInteger                                                   pendingErrorCount = new AtomicInteger(0);

    private final BusAddress                                                      busAddress;

    private final MessageFactory                                                  messageFactory;
    private final ConnectionConfig                                                connectionConfig;

    private volatile AbstractTransport                                            transport;

    private volatile boolean                                                      disconnecting;

    protected AbstractConnectionBase(ConnectionConfig _conCfg, TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        logger = LoggerFactory.getLogger(getClass());
        connectionConfig = Objects.requireNonNull(_conCfg, "Connection configuration required");

        exportedObjects = Collections.synchronizedMap(new HashMap<>());
        importedObjects = connectionConfig.isImportWeakReferences() ? Collections.synchronizedMap(new WeakHashMap<>()) : new ConcurrentHashMap<>();

        doWithExportedObjects(DBusException.class, eos -> eos.put(null, new ExportedObject(new GlobalHandler(this), false)));

        disconnecting = false;

        handledSignals = new ConcurrentHashMap<>();
        genericHandledSignals = new ConcurrentHashMap<>();

        pendingCalls = Collections.synchronizedMap(new LinkedHashMap<>());
        callbackManager = new PendingCallbackManager();

        pendingErrorQueue = new ConcurrentLinkedQueue<>();

        TransportBuilder transportBuilder = TransportBuilder.create(_transportConfig);
        busAddress = transportBuilder.getAddress();

        String senderThreadName = "DBus Sender Thread-";
        String rcvSvcName = "";
        if (logger.isDebugEnabled()) {
            senderThreadName = "DBus Sender Thread: " + busAddress.isListeningSocket() + ", ";
            rcvSvcName = "RcvSvc: " + busAddress.isListeningSocket() + " ";
        }

        receivingService = new ReceivingService(rcvSvcName, _rsCfg);
        senderService =
            Executors.newFixedThreadPool(1, new NameableThreadFactory(senderThreadName, true));

        objectTree = new ObjectTree();
        fallbackContainer = new FallbackContainer();

        readerThread = Objects.requireNonNull(createReaderThread(busAddress), "Reader thread required");

        try {
            transport = transportBuilder.build();
            messageFactory = Optional.ofNullable(transport)
                .map(AbstractTransport::getMessageFactory)
                .orElseThrow();
        } catch (IOException | DBusException _ex) {
            logger.debug("Error creating transport", _ex);
            if (_ex instanceof IOException ioe) {
                internalDisconnect(ioe);
            }
            throw new DBusException("Failed to connect to bus: " + _ex.getMessage(), _ex);
        }
    }

    /**
     * Retrieves an remote object using source and path.
     * Will try to find suitable exported DBusInterface automatically.
     *
     * @param _source source
     * @param _path path
     *
     * @return {@link DBusInterface} compatible object
     */
    public abstract DBusInterface getExportedObject(String _source, String _path) throws DBusException;

    /**
     * Retrieves an remote object using source and path.
     * Will use the given type as object class.
     *
     * @param _source source
     * @param _path path
     * @param _type class of remote object
     *
     * @return {@link DBusInterface} compatible object
     */
    public abstract <T extends DBusInterface> T getExportedObject(String _source, String _path, Class<T> _type) throws DBusException;

    /**
     * Create the read thread for reading incoming messages.
     *
     * @param _busAddress current bus address
     * @return IncomingMessageThread, never <code>null</code>
     */
    protected abstract IncomingMessageThread createReaderThread(BusAddress _busAddress);

    /**
     * The generated UUID of this machine.
     * @return String
     */
    public abstract String getMachineId();

    Message readIncoming() throws DBusException {
        if (!isConnected()) {
            return null;
        }
        Message m = null;
        try {
            m = getTransport().readMessage();
        } catch (IOException _exIo) {
            if (_exIo instanceof EOFException || _exIo instanceof ClosedByInterruptException) {

                Optional.ofNullable(getDisconnectCallback()).ifPresent(IDisconnectCallback::clientDisconnect);
                if (disconnecting // when we are already disconnecting, ignore further errors
                    || getBusAddress().isListeningSocket()) { // when we are listener, a client may disconnect any time which
                                                         // is no error
                    return null;
                }
            }

            if (isConnected()) {
                throw new FatalDBusException(_exIo);
            } // if run is false, suppress all exceptions - the connection either is already disconnected or should be disconnected right now
        }
        return m;
    }

    /**
     * Disconnects the DBus session. This method is final as it should never be overwritten by subclasses, otherwise
     * we have an endless recursion when using {@link #disconnect(IDisconnectAction, IDisconnectAction)} which then will
     * cause a StackOverflowError.
     *
     * @param _connectionError exception caused the disconnection (null if intended disconnect)
     */
    protected final synchronized void internalDisconnect(IOException _connectionError) {

        if (!isConnected()) { // already disconnected
            getLogger().debug("Ignoring disconnect, already disconnected");
            return;
        }
        disconnecting = true;

        getLogger().debug("Disconnecting Abstract Connection");

        Optional.ofNullable(getDisconnectCallback()).ifPresent(cb ->
            Optional.ofNullable(_connectionError)
                .ifPresentOrElse(cb::disconnectOnError, () -> cb.requestedDisconnect(null))
        );

        getImportedObjects().clear();

        // stop reading new messages
        readerThread.terminate();

        // terminate the signal handling pool
        receivingService.shutdown(10, TimeUnit.SECONDS);

        // stop potentially waiting method-calls
        Exception interrupt = _connectionError == null ? new IOException("Disconnecting") : _connectionError;

        synchronized (getPendingCalls()) {
            getLogger().debug("Notifying {} method call(s) to stop waiting for replies", getPendingCalls().size());
            for (MethodCall mthCall : getPendingCalls().values()) {
                try {
                    Error errorReply = getMessageFactory().createError(mthCall, interrupt);
                    mthCall.setReply(errorReply);
                    // also fail any registered async callback so it learns about the disconnect (and is removed -> no leak)
                    CallbackHandler<?> callback = getCallbackManager().removeCallback(mthCall);
                    if (callback != null) {
                        try {
                            callback.handleError(errorReply.getException());
                        } catch (Exception _ex) {
                            getLogger().debug("Exception while running disconnect error callback", _ex);
                        }
                    }
                } catch (DBusException _ex) {
                    getLogger().debug("Cannot set method reply to error", _ex);
                }
            }
            getPendingCalls().clear();
        }

        // shutdown sender executor service, send all remaining messages in main thread when no exception caused disconnection
        getLogger().debug("Shutting down SenderService");
        List<Runnable> remainingMsgsToSend = senderService.shutdownNow();
        // only try to send remaining messages when disconnection was not
        // caused by an IOException, otherwise we may block for method calls waiting for
        // reply which will never be received (due to disconnection by IOException)
        if (_connectionError == null) {
            for (Runnable runnable : remainingMsgsToSend) {
                runnable.run();
            }
        } else if (!remainingMsgsToSend.isEmpty()) {
            getLogger().debug("Will not send {} messages due to connection closed by IOException", remainingMsgsToSend.size());
        }

        // disconnect from the transport layer
        try {
            if (transport != null) {
                transport.close();
                transport = null;
            }
        } catch (IOException _ex) {
            getLogger().debug("Exception while disconnecting transport.", _ex);
        }

        // stop all the workers
        receivingService.shutdownNow();
        disconnecting = false;
    }

    /**
     * Special disconnect method which may be used whenever some cleanup before or after
     * disconnection to DBus is required.
     * @param _before action execute before actual disconnect, null if not needed
     * @param _after action execute after disconnect, null if not needed
     */
    protected synchronized void disconnect(IDisconnectAction _before, IDisconnectAction _after) {
        if (_before != null) {
            _before.perform();
        }
        internalDisconnect(null);
        if (_after != null) {
            _after.perform();
        }
    }

    /**
     * Disconnect from the Bus.
     */
    public synchronized void disconnect() {
        getLogger().debug("Disconnect called");
        internalDisconnect(null);
    }

    /**
     * Sends a reply on the bus to signal a non-existing property was requested.
     *
     * @param _methodCall method call
     * @param _params params
     *
     * @throws DBusException when sending fails
     */
    protected void rejectUnknownProperty(final MethodCall _methodCall, Object[] _params) throws DBusException {
        Object p = _params != null && _params.length >= 2 ? _params[1] : "unknown";
        sendMessage(getMessageFactory().createError(_methodCall, new UnknownProperty(String.format(
            "The property `%s' does not exist.", p))));
    }

    /**
     * Do some action with the currently exported objects in a synchronized manor.
     *
     * @param _exClz exception type which may be thrown
     * @param _action action to execute
     * @param <X> type of exception
     *
     * @return whatever the action returns
     *
     * @throws X thrown when action throws
     */
    protected <T, X extends Throwable> T doWithExportedObjectsAndReturn(Class<X> _exClz, IThrowingFunction<Map<String, ExportedObject>, T, X> _action) throws X {
        if (_action == null) {
            return null;
        }
        synchronized (exportedObjects) {
            return _action.apply(exportedObjects);
        }
    }

    /**
     * Do some action with the currently exported objects in a synchronized manor.
     * @param _exClz exception type which may be thrown
     * @param _action action to execute
     * @param <X> type of exception
     * @throws X thrown when action throws
     */
    protected <X extends Throwable> void doWithExportedObjects(Class<X> _exClz, IThrowingConsumer<Map<String, ExportedObject>, X> _action) throws X {
        if (_action == null) {
            return;
        }
        synchronized (exportedObjects) {
            _action.accept(exportedObjects);
        }
    }

    protected Logger getLogger() {
        return logger;
    }

    protected FallbackContainer getFallbackContainer() {
        return fallbackContainer;
    }

    /**
     * Returns the address this connection is connected to.
     *
     * @return new {@link BusAddress} object
     */
    public BusAddress getAddress() {
        return busAddress;
    }

    /**
     * Whether the transport is connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        AbstractTransport t = transport; // read volatile field once to avoid a check-then-act race
        return t != null && t.isConnected();
    }

    /**
     * The currently configured transport.
     *
     * @return AbstractTransport
     */
    protected AbstractTransport getTransport() {
        return transport;
    }

    /**
     * Send a message or signal to the DBus daemon.
     * @param _message message to send
     */
    public void sendMessage(Message _message) {
        if (!isConnected()) {
            throw new NotConnected("Cannot send message: Not connected");
        }

        Runnable runnable = () -> sendMessageInternally(_message);

        senderService.execute(runnable);
    }

    /**
     * Send a message to DBus.
     * @param _message message to send
     */
    private void sendMessageInternally(Message _message) {
        try {
            if (!isConnected()) {
                throw new NotConnected("Disconnected");
            }
            if (_message instanceof DBusSignal ds) {
                // update endianess if signal was created manually
                if (_message.getEndianess() == (byte) 0) {
                    _message.updateEndianess(getMessageFactory().getEndianess());
                }

                ds.appendbody(this);
            }

            if (_message instanceof MethodCall mc && 0 == (_message.getFlags() & Flags.NO_REPLY_EXPECTED) && null != getPendingCalls()) {
                synchronized (getPendingCalls()) {
                    getPendingCalls().put(_message.getSerial(), mc);
                }
            }

            getLogger().trace("Writing message to connection {}: {}", getTransport(), _message);
            getTransport().writeMessage(_message);

        } catch (Exception _ex) {
            getLogger().trace("Exception while sending message.", _ex);

            if (_message instanceof MethodCall mc && _ex instanceof DBusExecutionException) {
                try {
                    mc.setReply(getMessageFactory().createError(_message, _ex));
                } catch (DBusException _exDe) {
                    getLogger().trace("Could not set message reply", _exDe);
                }
            } else if (_message instanceof MethodCall mc) {
                try {
                    getLogger().info("Setting reply to {} as an error", _message);
                    mc.setReply(
                        getMessageFactory().createError(_message, new DBusExecutionException("Message Failed to Send: " + _ex.getMessage(), _ex)));
                } catch (DBusException _exDe) {
                    getLogger().trace("Could not set message reply", _exDe);
                }
            } else if (_message instanceof MethodReturn) {
                try {
                    getTransport().writeMessage(getMessageFactory().createError(_message, _ex));
                } catch (IOException | DBusException _exIo) {
                    getLogger().debug("Error writing method return to transport", _exIo);
                }
            }
            if (_ex instanceof IOException ioe) {
                getLogger().debug("Fatal IOException while sending message, disconnecting", _ex);
                internalDisconnect(ioe);
            }
        }
    }

    public String getExportedObject(DBusInterface _interface) throws DBusException {

        Optional<Entry<String, ExportedObject>> foundInterface = doWithExportedObjectsAndReturn(DBusException.class,
            eos ->
                eos.entrySet().stream()
                    .filter(e -> _interface.equals(e.getValue().getObject().get()))
                    .findFirst()
        );

        if (foundInterface.isPresent()) {
            return foundInterface.get().getKey();
        } else {
            RemoteObject rObj = getImportedObjects().get(_interface);
            if (rObj != null) {
                String s = rObj.getObjectPath();
                if (s != null) {
                    return s;
                }
            }

            throw new DBusException("Not an object exported or imported by this connection");
        }

    }

    /**
     * Return any DBus error which has been received.
     *
     * @return A DBusExecutionException, or null if no error is pending.
     */
    public DBusExecutionException getError() {
        Error poll = getPendingErrorQueue().poll();
        if (poll != null) {
            pendingErrorCount.decrementAndGet();
            return poll.getException();
        }
        return null;
    }

    /**
     * Adds an unhandled DBus error to the pending error queue in a bounded fashion.
     * <p>
     * The queue is only drained by callers of {@link #getError()}. Applications which never call {@code getError()}
     * would otherwise let this queue grow without limit. To avoid that, the queue is capped at
     * {@link #MAX_PENDING_ERRORS} entries; on overflow the oldest entry is dropped (the most recent errors, which are
     * usually the more relevant ones for diagnostics, are kept).
     * </p>
     *
     * @param _err error to enqueue
     */
    protected void addPendingError(Error _err) {
        if (offerBounded(getPendingErrorQueue(), pendingErrorCount, MAX_PENDING_ERRORS, _err)) {
            getLogger().debug("Pending error queue exceeded {} entries; dropped oldest unhandled error", MAX_PENDING_ERRORS);
        }
    }

    /**
     * Adds an element to a queue while keeping its size bounded, dropping the oldest element on overflow.
     * <p>
     * Uses the supplied {@link AtomicInteger} as an O(1) size counter instead of {@link Queue#size()} which is O(n) for
     * {@link ConcurrentLinkedQueue}. The counter must be maintained (decremented) by every other consumer that removes
     * elements from the same queue.
     * </p>
     *
     * @param <T> element type
     * @param _queue queue to add to
     * @param _count size counter associated with the queue
     * @param _max maximum number of elements to retain
     * @param _element element to add
     * @return {@code true} if an element was dropped to stay within the bound, {@code false} otherwise
     */
    static <T> boolean offerBounded(Queue<T> _queue, AtomicInteger _count, int _max, T _element) {
        _queue.add(_element);
        if (_count.incrementAndGet() > _max && _queue.poll() != null) {
            _count.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Connects the underlying transport if it is not already connected.
     * <p>
     * Will work for both, client and server (listening) connections.
     * </p>
     *
     * @return true if connection established or already connected, false otherwise
     * @throws IOException when connection was not already established and creating the connnection failed
     */
    public boolean connect() throws IOException {
        if (!getTransport().isConnected()) {
            if (getTransport().isListening()) {
                return getTransport().listen() != null;
            } else {
                return getTransport().connect() != null;
            }
        }
        return false;
    }

    /**
     * Returns the transport's configuration.<br>
     * Please note: changing any value will not change the transport settings!<br>
     * This is read-only.
     *
     * @return transport config
     */
    public TransportConfig getTransportConfig() {
        return getTransport().getTransportConfig();
    }

    /**
     * Start reading and messages.
     *
     * @throws IOException when listening fails
     */
    protected void listen() throws IOException {
        readerThread.start();
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    protected Queue<Error> getPendingErrorQueue() {
        return pendingErrorQueue;
    }

    protected Map<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> getHandledSignals() {
        return handledSignals;
    }

    protected Map<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>> getGenericHandledSignals() {
        return genericHandledSignals;
    }

    protected Map<Long, MethodCall> getPendingCalls() {
        return pendingCalls;
    }

    protected Map<DBusInterface, RemoteObject> getImportedObjects() {
        return importedObjects;
    }

    public ObjectTree getObjectTree() {
        return objectTree;
    }

    protected PendingCallbackManager getCallbackManager() {
        return callbackManager;
    }

    protected ReceivingService getReceivingService() {
        return receivingService;
    }

    protected BusAddress getBusAddress() {
        return busAddress;
    }

    protected Map<Thread, DBusCallInfo> getInfoMap() {
        return INFOMAP;
    }

    protected ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    /**
     * Stop Exporting an object
     *
     * @param _objectpath
     *            The objectpath to stop exporting.
     */
    public void unExportObject(String _objectpath) {
        doWithExportedObjects(null, eos -> {
            eos.remove(_objectpath);
            getObjectTree().remove(_objectpath);
        });
    }

    /**
     * Returns a structure with information on the current method call.
     *
     * @return the DBusCallInfo for this method call, or null if we are not in a method call.
     */
    public static DBusCallInfo getCallInfo() {
        return INFOMAP.get(Thread.currentThread());
    }

    /**
     * Returns the currently configured disconnect callback.
     *
     * @return callback or null if no callback registered
     */
    public IDisconnectCallback getDisconnectCallback() {
        return connectionConfig.getDisconnectCallback() == null ? null : connectionConfig.getDisconnectCallback();
    }

    /**
     * Disconnect this session (for use in try-with-resources).
     */
    @Override
    public void close() {
        disconnect();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[address=" + busAddress + "]";
    }

}
