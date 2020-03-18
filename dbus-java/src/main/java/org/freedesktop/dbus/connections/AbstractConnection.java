/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.connections;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.InternalSignal;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.SignalTuple;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalDBusException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.messages.ObjectTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.threads.NameableThreadFactory;

/**
 * Handles a connection to DBus.
 */
public abstract class AbstractConnection implements Closeable {

    private static final Map<Thread, DBusCallInfo> INFOMAP     = new ConcurrentHashMap<>();
    /**
     * Default thread pool size
     */
    private static final int         THREADCOUNT = 4;
    /**
     * Connect timeout, used for TCP only
     */
    public static final int          TCP_CONNECT_TIMEOUT     = 100000;

    /** Lame method to setup endianness used on DBus messages */
    private static byte              endianness       = getSystemEndianness();

    public static final boolean      FLOAT_SUPPORT    =    (null != System.getenv("DBUS_JAVA_FLOATS"));
    public static final String       BUSNAME_REGEX    = "^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$";
    public static final String       CONNID_REGEX     = "^:[0-9]*\\.[0-9]*$";
    public static final String       OBJECT_REGEX     = "^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$";
    public static final Pattern      DOLLAR_PATTERN   = Pattern.compile("[$]");

    public static final int          MAX_ARRAY_LENGTH = 67108864;
    public static final int          MAX_NAME_LENGTH  = 255;

    private final Logger        logger = LoggerFactory.getLogger(getClass());

    private final ObjectTree                                                    objectTree;

    private final Map<String, ExportedObject>                                   exportedObjects;
    private final Map<DBusInterface, RemoteObject>                              importedObjects;

    private final PendingCallbackManager                                        callbackManager;

    private final FallbackContainer                                             fallbackContainer;

    private final Queue<Error>                                                  pendingErrorQueue;

    private final Map<SignalTuple, Queue<DBusSigHandler<? extends DBusSignal>>> handledSignals;
    private final Map<SignalTuple, Queue<DBusSigHandler<DBusSignal>>>           genericHandledSignals;
    private final Map<Long, MethodCall>                                         pendingCalls;

    private final IncomingMessageThread                                         readerThread;
    // private final SenderThread senderThread;

    private final BusAddress                                                    busAddress;

    private final ExecutorService                                               senderService;

    private volatile boolean                                                    run;

    private boolean                                                             weakreferences       = false;
    private boolean                                                             connected            = false;

    private AbstractTransport                                                   transport;
    private volatile ThreadPoolExecutor                                         workerThreadPool;
    private final ReadWriteLock                                                 workerThreadPoolLock =
            new ReentrantReadWriteLock();

    protected AbstractConnection(String address, int timeout) throws DBusException {
        exportedObjects = new HashMap<>();
        importedObjects = new ConcurrentHashMap<>();

        exportedObjects.put(null, new ExportedObject(new GlobalHandler(this), weakreferences));

        handledSignals = new ConcurrentHashMap<>();
        genericHandledSignals = new ConcurrentHashMap<>();
        pendingCalls = Collections.synchronizedMap(new LinkedHashMap<>());
        callbackManager = new PendingCallbackManager();

        pendingErrorQueue = new ConcurrentLinkedQueue<>();
        workerThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADCOUNT,
                        new NameableThreadFactory("DBus Worker Thread-", false));

        senderService =
                Executors.newFixedThreadPool(1, new NameableThreadFactory("DBus Sender Thread-", false));

        objectTree = new ObjectTree();
        fallbackContainer = new FallbackContainer();

        readerThread = new IncomingMessageThread(this);

        try {
            busAddress = new BusAddress(address);
            transport = TransportFactory.createTransport(busAddress, timeout);
            connected = true;
        } catch (IOException | DBusException _ex) {
            logger.debug("Error creating transport", _ex);
            disconnect();
            throw new DBusException("Failed to connect to bus: " + _ex.getMessage(), _ex);
        }
        run = true;

    }

    public abstract DBusInterface getExportedObject(String source, String path) throws DBusException;

    /**
     * Remove a match rule with the given {@link DBusSigHandler}.
     * The rule will only be removed from DBus if no other additional handlers are registered to the same rule.
     *
     * @param _rule rule to remove
     * @param _handler handler to remove
     * @throws DBusException on error
     */
    protected abstract <T extends DBusSignal> void removeSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException;

    /**
     * Add a signal handler with the given {@link DBusMatchRule} to DBus.
     * The rule will be added to DBus if it was not added before.
     * If the rule was already added, the signal handler is added to the internal map receiving
     * the same signal as the first (and additional) handlers for this rule.
     *
     * @param _rule rule to add
     * @param _handler handler to use
     * @throws DBusException on error
     */
    protected abstract <T extends DBusSignal> void addSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException;

    /**
     * Remove a generic signal handler with the given {@link DBusMatchRule}.
     * The rule will only be removed from DBus if no other additional handlers are registered to the same rule.
     *
     * @param _rule rule to remove
     * @param _handler handler to remove
     * @throws DBusException on error
     */
    protected abstract void removeGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException;

    /**
     * Adds a {@link DBusMatchRule} to with a generic signal handler.
     * Generic signal handlers allow receiving different signals with the same handler.
     * If the rule was already added, the signal handler is added to the internal map receiving
     * the same signal as the first (and additional) handlers for this rule.
     *
     * @param _rule rule to add
     * @param _handler handler to use
     * @throws DBusException on error
     */
    protected abstract void addGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException;

    /**
     * The generated UUID of this machine.
     * @return String
     */
    public abstract String getMachineId();

    /**
     * Start reading and sending messages.
     */
    protected void listen() {
        readerThread.start();
    }

    /**
     * Change the number of worker threads to receive method calls and handle signals. Default is 4 threads
     *
     * @param _newPoolSize
     *            The new number of worker Threads to use.
     */
    public void changeThreadCount(byte _newPoolSize) {
        if (workerThreadPool.getMaximumPoolSize() != _newPoolSize) {
            workerThreadPoolLock.writeLock().lock();
            try {
                List<Runnable> remainingTasks = workerThreadPool.shutdownNow(); // kill previous threadpool
                workerThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(_newPoolSize,
                    new NameableThreadFactory("DbusWorkerThreads", false));
                // re-schedule previously waiting tasks
                for (Runnable runnable : remainingTasks) {
                    workerThreadPool.execute(runnable);
                }
            } finally {
                workerThreadPoolLock.writeLock().unlock();
            }

        }
    }

    public String getExportedObject(DBusInterface _interface) throws DBusException {

        Optional<Entry<String, ExportedObject>> foundInterface = 
        		getExportedObjects().entrySet().stream()
        			.filter(e -> _interface.equals(e.getValue().getObject().get()))
        			.findFirst();
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
     * If set to true the bus will not hold a strong reference to exported objects. If they go out of scope they will
     * automatically be unexported from the bus. The default is to hold a strong reference, which means objects must be
     * explicitly unexported before they will be garbage collected.
     *
     * @param _weakreferences
     *            reference
     */
    public void setWeakReferences(boolean _weakreferences) {
        this.weakreferences = _weakreferences;
    }

    /**
     * Export an object so that its methods can be called on DBus.
     *
     * @param objectpath
     *            The path to the object we are exposing. MUST be in slash-notation, like "/org/freedesktop/Local", and
     *            SHOULD end with a capitalised term. Only one object may be exposed on each path at any one time, but
     *            an object may be exposed on several paths at once.
     * @param object
     *            The object to export.
     * @throws DBusException
     *             If the objectpath is already exporting an object. or if objectpath is incorrectly formatted,
     */
    public void exportObject(String objectpath, DBusInterface object) throws DBusException {
        if (null == objectpath || "".equals(objectpath)) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        synchronized (getExportedObjects()) {
            if (null != getExportedObjects().get(objectpath)) {
                throw new DBusException("Object already exported");
            }
            ExportedObject eo = new ExportedObject(object, weakreferences);
            getExportedObjects().put(objectpath, eo);
            synchronized (getObjectTree()) {
                getObjectTree().add(objectpath, eo, eo.getIntrospectiondata());
            }
        }
    }

    /**
     * Export an object as a fallback object. This object will have it's methods invoked for all paths starting with
     * this object path.
     *
     * @param _objectPrefix
     *            The path below which the fallback handles calls. MUST be in slash-notation, like
     *            "/org/freedesktop/Local",
     * @param _object
     *            The object to export.
     * @throws DBusException
     *             If the objectpath is incorrectly formatted,
     */
    public void addFallback(String _objectPrefix, DBusInterface _object) throws DBusException {
        if (null == _objectPrefix || "".equals(_objectPrefix)) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (!_objectPrefix.matches(OBJECT_REGEX) || _objectPrefix.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + _objectPrefix);
        }
        ExportedObject eo = new ExportedObject(_object, weakreferences);
        fallbackContainer.add(_objectPrefix, eo);
    }

    /**
     * Remove a fallback
     *
     * @param _objectprefix
     *            The prefix to remove the fallback for.
     */
    public void removeFallback(String _objectprefix) {
        fallbackContainer.remove(_objectprefix);
    }

    /**
     * Stop Exporting an object
     *
     * @param _objectpath
     *            The objectpath to stop exporting.
     */
    public void unExportObject(String _objectpath) {
        synchronized (getExportedObjects()) {
            getExportedObjects().remove(_objectpath);
            getObjectTree().remove(_objectpath);
        }
    }

    /**
     * Send a message or signal to the DBus daemon.
     * @param _message message to send
     */
    public void sendMessage(Message _message) {
    	Runnable runnable = new Runnable() {
			@Override
			public void run() {
				sendMessageInternally(_message);
			}
    	};

    	senderService.execute(runnable);
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }

        removeSigHandler(new DBusMatchRule(type), handler);
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param object
     *            The object emitting the signal.
     * @param handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        String objectpath = getImportedObjects().get(object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        removeSigHandler(new DBusMatchRule(type, null, objectpath), handler);
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type
     * and name.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param handler
     *            The handler to call when a signal is received.
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        addSigHandler(new DBusMatchRule(type), handler);
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type,
     * name and object.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param object
     *            The object from which the signal will be emitted
     * @param handler
     *            The handler to call when a signal is received.
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        RemoteObject rObj = getImportedObjects().get(object);
        if (rObj == null) {
            throw new DBusException("Not an object exported or imported by this connection");
        }
        String objectpath = rObj.getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        addSigHandler(new DBusMatchRule(type, null, objectpath), handler);
    }

    protected <T extends DBusSignal> void addSigHandlerWithoutMatch(Class<? extends DBusSignal> signal,
            DBusSigHandler<T> handler) throws DBusException {
        DBusMatchRule rule = new DBusMatchRule(signal);
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        synchronized (getHandledSignals()) {
            Queue<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(key);
            if (null == v) {
                v = new ConcurrentLinkedQueue<>();
                v.add(handler);
                getHandledSignals().put(key, v);
            } else {
                v.add(handler);
            }
        }
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
        internalDisconnect();
        if (_after != null) {
            _after.perform();
        }
    }

    /**
     * Disconnects the DBus session.
     * This method is private as it should never be overwritten by subclasses,
     * otherwise we have an endless recursion when using {@link #disconnect(IDisconnectAction, IDisconnectAction)}
     * which then will cause a StackOverflowError.
     */
    private synchronized void internalDisconnect() {

        if (connected == false) { // already disconnected
            return;
        }

        logger.debug("Sending disconnected signal");
        try {
            handleMessage(new org.freedesktop.dbus.interfaces.Local.Disconnected("/"), false);
        } catch (Exception ex) {
            logger.debug("Exception while disconnecting", ex);
        }


        logger.debug("Disconnecting Abstract Connection");

        workerThreadPoolLock.writeLock().lock();
        try {
            // try to wait for all pending tasks.
            workerThreadPool.shutdown();
            workerThreadPool.awaitTermination(10, TimeUnit.SECONDS); // 10 seconds should be enough, otherwise fail

        } catch (InterruptedException _ex) {
            logger.error("Interrupted while waiting for worker threads to be terminated.", _ex);
        } finally {
            workerThreadPoolLock.writeLock().unlock();
        }

        // shutdown sender executor service, send all remaining messages in main thread
        for (Runnable runnable : senderService.shutdownNow()) {
            runnable.run();
        }

        // stop the main thread
        run = false;
        connected = false;

        readerThread.setTerminate(true);

        // disconnect from the transport layer
        try {
            if (transport != null) {
                transport.close();
                transport = null;
            }
        } catch (IOException exIo) {
            logger.debug("Exception while disconnecting transport.", exIo);
        }

        // stop all the workers
        workerThreadPoolLock.writeLock().lock();
        try {
            if (!workerThreadPool.isTerminated()) { // try forceful shutdown
                workerThreadPool.shutdownNow();
            }
        } finally {
            workerThreadPoolLock.writeLock().unlock();
        }
    }

    /**
     * Disconnect from the Bus.
     */
    public synchronized void disconnect() {
        internalDisconnect();
    }

    /**
     * Disconnect this session (for use in try-with-resources).
     */
    @Override
    public void close() throws IOException {
        disconnect();
    }

    /**
     * Call a method asynchronously and set a callback. This handler will be called in a separate thread.
     *
     * @param <A>
     *            whatever
     * @param object
     *            The remote object on which to call the method.
     * @param m
     *            The name of the method on the interface to call.
     * @param callback
     *            The callback handler.
     * @param parameters
     *            The parameters to call the method with.
     */
    public <A> void callWithCallback(DBusInterface object, String m, CallbackHandler<A> callback,
            Object... parameters) {
        logger.trace("callWithCallback({}, {}, {})", object, m, callback);
        Class<?>[] types = createTypesArray( parameters );
        RemoteObject ro = getImportedObjects().get(object);

        try {
            Method me;
            if (null == ro.getInterface()) {
                me = object.getClass().getMethod(m, types);
            } else {
                me = ro.getInterface().getMethod(m, types);
            }
            RemoteInvocationHandler.executeRemoteMethod(ro, me, this, RemoteInvocationHandler.CALL_TYPE_CALLBACK,
                    callback, parameters);
        } catch (DBusExecutionException exEe) {
            logger.debug("", exEe);
            throw exEe;
        } catch (Exception e) {
            logger.debug("", e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

    /**
     * Call a method asynchronously and get a handle with which to get the reply.
     *
     * @param object
     *            The remote object on which to call the method.
     * @param m
     *            The name of the method on the interface to call.
     * @param parameters
     *            The parameters to call the method with.
     * @return A handle to the call.
     */
    public DBusAsyncReply<?> callMethodAsync(DBusInterface object, String m, Object... parameters) {
        Class<?>[] types = createTypesArray( parameters );
        RemoteObject ro = getImportedObjects().get(object);

        try {
            Method me;
            if (null == ro.getInterface()) {
                me = object.getClass().getMethod(m, types);
            } else {
                me = ro.getInterface().getMethod(m, types);
            }
            return (DBusAsyncReply<?>) RemoteInvocationHandler.executeRemoteMethod(ro, me, this,
                    RemoteInvocationHandler.CALL_TYPE_ASYNC, null, parameters);
        } catch (DBusExecutionException exDee) {
            logger.debug("", exDee);
            throw exDee;
        } catch (Exception e) {
            logger.debug("", e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

    private Class<?>[] createTypesArray(Object... parameters) {
        if (parameters == null) {
            return null;
        }
        return Arrays.stream(parameters)
                .filter(p -> p != null) // do no try to convert null values to concrete class
                .map(p -> {
                    if (List.class.isAssignableFrom(p.getClass())) { // turn possible List subclasses (e.g. ArrayList) to interface class List
                        return List.class;
                    } else if (Map.class.isAssignableFrom(p.getClass())) { // do the same for Map subclasses
                        return Map.class;
                    } else if (Set.class.isAssignableFrom(p.getClass())) { // and also for Set subclasses
                        return Set.class;
                    } else {
                        return p.getClass();
                    }
                })
                .toArray(Class[]::new);
    }

    protected void handleException(AbstractConnection dbusConnection, Message methodOrSignal,
            DBusExecutionException exception) {
        if (dbusConnection == null) {
            throw new NullPointerException("DBusConnection cannot be null");
        }
        try {
            dbusConnection.sendMessage(new Error(methodOrSignal, exception));
        } catch (DBusException ex) {
            logger.warn("Exception caught while processing previous error.", ex);
        }
    }

    /**
     * Handle received message from DBus.
     * @param _message
     * @throws DBusException
     */
    void handleMessage(Message _message) throws DBusException {
        if (_message instanceof DBusSignal) {
            handleMessage((DBusSignal) _message, true);
        } else if (_message instanceof MethodCall) {
            handleMessage((MethodCall) _message);
        } else if (_message instanceof MethodReturn) {
            handleMessage((MethodReturn) _message);
        } else if (_message instanceof Error) {
            handleMessage((Error) _message);
        }
    }

    private void handleMessage(final MethodCall m) throws DBusException {
        logger.debug("Handling incoming method call: {}", m);

        ExportedObject eo = null;
        Method meth = null;
        Object o = null;

        if (null == m.getInterface() || m.getInterface().equals("org.freedesktop.DBus.Peer")
                || m.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            eo = getExportedObjects().get(null);
            if (null != eo && null == eo.getObject().get()) {
                unExportObject(null);
                eo = null;
            }
            if (null != eo) {
                meth = eo.getMethods().get(new MethodTuple(m.getName(), m.getSig()));
            }
            if (null != meth) {
                o = new GlobalHandler(this, m.getPath());
            } else {
                eo = null;
            }
        }
        if (null == o) {
            // now check for specific exported functions

            eo = getExportedObjects().get(m.getPath());
            if (null != eo && null == eo.getObject().get()) {
                logger.info("Unexporting {} implicitly", m.getPath());
                unExportObject(m.getPath());
                eo = null;
            }

            if (null == eo) {
                eo = fallbackContainer.get(m.getPath());
            }

            if (null == eo) {
                sendMessage(new Error(m,
                        new UnknownObject(m.getPath() + " is not an object provided by this process.")));
                return;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Searching for method {}  with signature {}", m.getName(), m.getSig());
                logger.trace("List of methods on {}: ", eo);
                for (MethodTuple mt : eo.getMethods().keySet()) {
                    logger.trace("   {} => {}", mt, eo.getMethods().get(mt));
                }
            }
            meth = eo.getMethods().get(new MethodTuple(m.getName(), m.getSig()));
            if (null == meth) {
                sendMessage(new Error(m, new UnknownMethod(String.format(
                        "The method `%s.%s' does not exist on this object.", m.getInterface(), m.getName()))));
                return;
            }
            o = eo.getObject().get();
        }

        // now execute it
        final Method me = meth;
        final Object ob = o;
        final boolean noreply = (1 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED));
        final DBusCallInfo info = new DBusCallInfo(m);
        final AbstractConnection conn = this;

        logger.trace("Adding Runnable for method {}", meth);
        Runnable r = new Runnable() {

            @Override
            public void run() {
                logger.debug("Running method {} for remote call", me);
                if (me == null) {
                	logger.debug("Cannot run method - method variable was null");
                	return;
                }
                try {
                    Type[] ts = me.getGenericParameterTypes();
                    m.setArgs(Marshalling.deSerializeParameters(m.getParameters(), ts, conn));
                    logger.trace("Deserialised {} to types {}", Arrays.deepToString(m.getParameters()), Arrays.deepToString(ts));
                } catch (Exception e) {
                    logger.debug("", e);
                    handleException(conn, m, new UnknownMethod("Failure in de-serializing message: " + e));
                    return;
                }

                try {
                    INFOMAP.put(Thread.currentThread(), info);
                    Object result;
                    try {
                        logger.trace("Invoking Method: {} on {} with parameters {}", me, ob, Arrays.deepToString(m.getParameters()));
                        result = me.invoke(ob, m.getParameters());
                    } catch (InvocationTargetException ite) {
                        logger.debug(ite.getMessage(), ite);
                        throw ite.getCause();
                    }
                    INFOMAP.remove(Thread.currentThread());
                    if (!noreply) {
                        MethodReturn reply;
                        if (Void.TYPE.equals(me.getReturnType())) {
                            reply = new MethodReturn(m, null);
                        } else {
                            StringBuffer sb = new StringBuffer();
                            for (String s : Marshalling.getDBusType(me.getGenericReturnType())) {
                                sb.append(s);
                            }
                            Object[] nr = Marshalling.convertParameters(new Object[] {
                                    result
                            }, new Type[] {
                                    me.getGenericReturnType()
                            }, conn);

                            reply = new MethodReturn(m, sb.toString(), nr);
                        }
                        conn.sendMessage(reply);
                    }
                } catch (DBusExecutionException exDee) {
                    logger.debug("", exDee);
                    handleException(conn, m, exDee);
                } catch (Throwable e) {
                    logger.debug("", e);
                    handleException(conn, m,
                            new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                                    m.getInterface(), m.getName(), e.getMessage())));
                }
            }
        };
        executeInWorkerThreadPool(r);
    }

    /**
     * Handle a signal received on DBus.
     *
     * @param _signal signal to handle
     * @param _useThreadPool whether to handle this signal in another thread or handle it byself
     */
    @SuppressWarnings({
            "unchecked"
    })
    private void handleMessage(final DBusSignal _signal, boolean _useThreadPool) {
        logger.debug("Handling incoming signal: {}", _signal);

        List<DBusSigHandler<? extends DBusSignal>> handlers = new ArrayList<>();
        List<DBusSigHandler<DBusSignal>> genericHandlers = new ArrayList<>();

        Queue<DBusSigHandler<? extends DBusSignal>> t;
        t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), null, null));
        if (null != t) {
            handlers.addAll(t);
        }
        t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), _signal.getPath(), null));
        if (null != t) {
            handlers.addAll(t);
        }
        t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), null, _signal.getSource()));
        if (null != t) {
            handlers.addAll(t);
        }
        t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), _signal.getPath(), _signal.getSource()));
        if (null != t) {
            handlers.addAll(t);
            }

        Queue<DBusSigHandler<DBusSignal>> gt;
        Set<SignalTuple> allTuples = SignalTuple.getAllPossibleTuples(_signal.getInterface(), _signal.getName(), _signal.getPath(), _signal.getSource());
        for( SignalTuple tuple : allTuples ){
           gt = getGenericHandledSignals().get(tuple);
            if (null != gt) {
                genericHandlers.addAll(gt);
            }
        }

        if (handlers.isEmpty() && genericHandlers.isEmpty()) {
            return;
        }

        final AbstractConnection conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : handlers) {
            logger.trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = new Runnable() {

                @Override
                public void run() {
                    try {
                        DBusSignal rs;
                        if (_signal instanceof InternalSignal || _signal.getClass().equals(DBusSignal.class)) {
                            rs = _signal.createReal(conn);
                        } else {
                            rs = _signal;
                        }
                        ((DBusSigHandler<DBusSignal>) h).handle(rs);
                    } catch (DBusException _ex) {
                        logger.warn("Exception while running signal handler '{}' for signal '{}':", h, _signal, _ex);
                        handleException(conn, _signal, new DBusExecutionException("Error handling signal " + _signal.getInterface()
                                + "." + _signal.getName() + ": " + _ex.getMessage()));
                    }
                }
            };
            if (_useThreadPool) {
                executeInWorkerThreadPool(command);
            } else {
                command.run();
            }
        }

        for (final DBusSigHandler<DBusSignal> h : genericHandlers) {
            logger.trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = new Runnable() {

                @Override
                public void run() {
                    h.handle(_signal);
                }
            };
            if (_useThreadPool) {
                executeInWorkerThreadPool(command);
            } else {
                command.run();
            }
        }
    }

    private void executeInWorkerThreadPool(Runnable task) {
        workerThreadPoolLock.readLock().lock();
        try {
            workerThreadPool.execute(task);
        } finally {
            workerThreadPoolLock.readLock().unlock();
        }
    }

    private void handleMessage(final Error err) {
        logger.debug("Handling incoming error: {}", err);
        MethodCall m = null;
        if (getPendingCalls() == null) {
            return;
        }
        synchronized (getPendingCalls()) {
            if (getPendingCalls().containsKey(err.getReplySerial())) {
                m = getPendingCalls().remove(err.getReplySerial());
            }
        }
        if (m != null) {
            m.setReply(err);
            CallbackHandler<?> cbh = null;
            cbh = callbackManager.removeCallback(m);
            logger.trace("{} = pendingCallbacks.remove({})", cbh, m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<?> fcbh = cbh;
                logger.trace("Adding Error Runnable with callback handler {}", fcbh);
                Runnable command = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            logger.trace("Running Error Callback for {}", err);
                            DBusCallInfo info = new DBusCallInfo(err);
                            INFOMAP.put(Thread.currentThread(), info);

                            fcbh.handleError(err.getException());
                            INFOMAP.remove(Thread.currentThread());

                        } catch (Exception e) {
                            logger.debug("Exception while running error callback.", e);
                        }
                    }
                };
                executeInWorkerThreadPool(command);
            }

        } else {
            getPendingErrorQueue().add(err);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(final MethodReturn mr) {
        logger.debug("Handling incoming method return: {}", mr);
        MethodCall m = null;

        if (null == getPendingCalls()) {
            return;
        }

        synchronized (getPendingCalls()) {
            if (getPendingCalls().containsKey(mr.getReplySerial())) {
                m = getPendingCalls().remove(mr.getReplySerial());
            }
        }

        if (null != m) {
            m.setReply(mr);
            mr.setCall(m);
            @SuppressWarnings("rawtypes")
            CallbackHandler cbh = callbackManager.getCallback(m);
            DBusAsyncReply<?> asr = callbackManager.getCallbackReply(m);
            callbackManager.removeCallback(m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<Object> fcbh = cbh;
                final DBusAsyncReply<?> fasr = asr;
                if (fasr == null) {
                	logger.debug("Cannot add runnable for method, given method callback was null");
                	return;
                }
                logger.trace("Adding Runnable for method {} with callback handler {}", fcbh,
                        fasr != null ? fasr.getMethod() : null);
                Runnable r = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            logger.trace("Running Callback for {}", mr);
                            DBusCallInfo info = new DBusCallInfo(mr);
                            INFOMAP.put(Thread.currentThread(), info);
                            Object convertRV = RemoteInvocationHandler.convertRV(mr.getSig(), mr.getParameters(),
                                    fasr.getMethod(), fasr.getConnection());
                            fcbh.handle(convertRV);
                            INFOMAP.remove(Thread.currentThread());

                        } catch (Exception e) {
                            logger.debug("Exception while running callback.", e);
                        }
                    }
                };
                executeInWorkerThreadPool(r);
            }

        } else {
            try {
                sendMessage(new Error(mr, new DBusExecutionException(
                        "Spurious reply. No message with the given serial id was awaiting a reply.")));
            } catch (DBusException exDe) {
            }
        }
    }

    public void queueCallback(MethodCall _call, Method _method, CallbackHandler<?> _callback) {
        callbackManager.queueCallback(_call, _method, _callback, this);
    }

    /**
     * Send a message to DBus.
     * @param m
     */
    private void sendMessageInternally(Message m) {
        try {
            if (!connected) {
                throw new NotConnected("Disconnected");
            }
            if (m instanceof DBusSignal) {
                ((DBusSignal) m).appendbody(this);
            }

            if (m instanceof MethodCall) {
                if (0 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED)) {
                    if (null == getPendingCalls()) {
                        ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local",
                                "org.freedesktop.DBus.Local.Disconnected", 0, "s", "Disconnected"));
                    } else {
                        synchronized (getPendingCalls()) {
                            getPendingCalls().put(m.getSerial(), (MethodCall) m);
                        }
                    }
                }
            }

            transport.writeMessage(m);

        } catch (Exception e) {
            logger.debug("Exception while sending message.", e);
            if (m instanceof MethodCall && e instanceof NotConnected) {
                try {
                    ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local",
                            "org.freedesktop.DBus.Local.Disconnected", 0, "s", "Disconnected"));
                } catch (DBusException exDe) {
                }
            }
            if (m instanceof MethodCall && e instanceof DBusExecutionException) {
                try {
                    ((MethodCall) m).setReply(new Error(m, e));
                } catch (DBusException exDe) {
                }
            } else if (m instanceof MethodCall) {
                try {
                    logger.info("Setting reply to {} as an error", m);
                    ((MethodCall) m).setReply(
                            new Error(m, new DBusExecutionException("Message Failed to Send: " + e.getMessage())));
                } catch (DBusException exDe) {
                }
            } else if (m instanceof MethodReturn) {
                try {
                    transport.writeMessage(new Error(m, e));
                } catch (IOException exIo) {
                    logger.debug("", exIo);
                } catch (DBusException exDe) {
                    logger.debug("", exDe);
                }
            }
            if (e instanceof IOException) {
                disconnect();
            }
        }
    }

    Message readIncoming() throws DBusException {
        if (!connected) {
            //throw new NotConnected("No transport present");
            return null;
        }
        Message m = null;
        try {
            m = transport.readMessage();
        } catch (IOException exIo) {
            if (!run && (exIo instanceof EOFException)) { // EOF is expected when connection is shutdown
                return null;
            }
            if (run) {
                throw new FatalDBusException(exIo.getMessage());
            } // if run is false, suppress all exceptions - the connection either is already disconnected or should be disconnected right now
        }
        return m;
    }

    protected Map<String, ExportedObject> getExportedObjects() {
        return exportedObjects;
    }

    FallbackContainer getFallbackContainer() {
        return fallbackContainer;
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
     * Return any DBus error which has been received.
     *
     * @return A DBusExecutionException, or null if no error is pending.
     */
    public DBusExecutionException getError() {
        Error poll = getPendingErrorQueue().poll();
        if (poll != null) {
            return poll.getException();
        }
        return null;
    }

    /**
     * Returns the address this connection is connected to.
     *
     * @return new {@link BusAddress} object
     */
    public BusAddress getAddress() {
        return busAddress;
    }

    public boolean isConnected() {
        return connected;
    }

    protected Queue<Error> getPendingErrorQueue() {
        return pendingErrorQueue;
    }

    protected Map<SignalTuple, Queue<DBusSigHandler<? extends DBusSignal>>> getHandledSignals() {
        return handledSignals;
    }

    protected Map<SignalTuple, Queue<DBusSigHandler<DBusSignal>>> getGenericHandledSignals() {
        return genericHandledSignals;
    }

    protected Map<Long, MethodCall> getPendingCalls() {
        return pendingCalls;
    }

    protected Map<DBusInterface, RemoteObject> getImportedObjects() {
        return importedObjects;
    }

    protected ObjectTree getObjectTree() {
        return objectTree;
    }

    /**
     * Set the endianness to use for all connections.
     * Defaults to the system architectures endianness.
     *
     * @param _b Message.Endian.BIG or Message.Endian.LITTLE
     */
    public static void setEndianness(byte _b) {
        if (_b == Message.Endian.BIG || _b == Message.Endian.LITTLE) {
            endianness = _b;
        }
    }

    /**
     * Get current endianness to use.
     * @return Message.Endian.BIG or Message.Endian.LITTLE
     */
    public static byte getEndianness() {
        return endianness;
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
