/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.connections;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.ParseException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
     * Timeout in µs on checking the BUS for incoming messages and sending outgoing messages
     */
    private static final int                       TIMEOUT     = 100000;
    /**
     * Default thread pool size
     */
    private static final int         THREADCOUNT = 4;

    public static final boolean      FLOAT_SUPPORT    =    (null != System.getenv("DBUS_JAVA_FLOATS"));
    public static final String       BUSNAME_REGEX    = "^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$";
    public static final String       CONNID_REGEX     = "^:[0-9]*\\.[0-9]*$";
    public static final String       OBJECT_REGEX     = "^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$";
    public static final Pattern      DOLLAR_PATTERN   = Pattern.compile("[$]");

    public static final int          MAX_ARRAY_LENGTH = 67108864;
    public static final int          MAX_NAME_LENGTH  = 255;

    private final Logger        logger = LoggerFactory.getLogger(getClass());

    private final ObjectTree                                                   objectTree;

    private final Map<String, ExportedObject>                                  exportedObjects;
    private final Map<DBusInterface, RemoteObject>                             importedObjects;

    private final PendingCallbackManager                                       callbackManager;

    private final FallbackContainer                                            fallbackContainer;

    private final Queue<Error>                                                 pendingErrorQueue;

    private final Map<SignalTuple, List<DBusSigHandler<? extends DBusSignal>>> handledSignals;
    private final Map<Long, MethodCall>                                        pendingCalls;

    private final IncomingMessageThread                                        readerThread;
    //private final SenderThread                                                 senderThread;

    private final BusAddress                                                   busAddress;

    private volatile boolean                                                   run;

    private boolean                                                            weakreferences   = false;
    private boolean                                                            connected        = false;

    private Transport                                                          transport;
    private ExecutorService                                                    workerThreadPool;
    private ExecutorService                                                    senderService;

    protected AbstractConnection(String address) throws DBusException {
        exportedObjects = new HashMap<>();
        importedObjects = new ConcurrentHashMap<>();

        exportedObjects.put(null, new ExportedObject(new GlobalHandler(this), weakreferences));
        
        handledSignals = new ConcurrentHashMap<>();
        pendingCalls = Collections.synchronizedMap(new LinkedHashMap<>());
        callbackManager = new PendingCallbackManager();

        pendingErrorQueue = new ConcurrentLinkedQueue<>();
        workerThreadPool =
                Executors.newFixedThreadPool(THREADCOUNT, new NameableThreadFactory("DBus Worker Thread-", false));

        senderService =
                Executors.newFixedThreadPool(1, new NameableThreadFactory("DBus Sender Thread-", false));
        
        objectTree = new ObjectTree();
        fallbackContainer = new FallbackContainer();

        readerThread = new IncomingMessageThread(this);

        try {
            busAddress = new BusAddress(address);
            transport = new Transport(busAddress, AbstractConnection.TIMEOUT);
            connected = true;
        } catch (IOException | DBusException ioe) {
            logger.debug("Error creating transport", ioe);
            disconnect();
            throw new DBusException("Failed to connect to bus " + ioe.getMessage());
        }
        run = true;

    }

    public abstract DBusInterface getExportedObject(String source, String path) throws DBusException;

    protected abstract <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

    protected abstract <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

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
     * @param newcount
     *            The new number of worker Threads to use.
     */
    public void changeThreadCount(byte newcount) {
        if (newcount != THREADCOUNT) {
            List<Runnable> remainingTasks = workerThreadPool.shutdownNow(); // kill previous threadpool
            workerThreadPool =
                    Executors.newFixedThreadPool(newcount, new NameableThreadFactory("DbusWorkerThreads", false));
            // re-schedule previously waiting tasks
            for (Runnable runnable : remainingTasks) {
                workerThreadPool.execute(runnable);
            }
        }
    }

    public String getExportedObject(DBusInterface _interface) throws DBusException {
        
        Optional<Entry<String, ExportedObject>> foundInterface = getExportedObjects().entrySet().stream().filter(e -> _interface.equals(e.getValue().getObject().get())).findFirst();
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
     * @param objectprefix
     *            The path below which the fallback handles calls. MUST be in slash-notation, like
     *            "/org/freedesktop/Local",
     * @param object
     *            The object to export.
     * @throws DBusException
     *             If the objectpath is incorrectly formatted,
     */
    public void addFallback(String objectprefix, DBusInterface object) throws DBusException {
        if (null == objectprefix || "".equals(objectprefix)) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (!objectprefix.matches(OBJECT_REGEX) || objectprefix.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectprefix);
        }
        ExportedObject eo = new ExportedObject(object, weakreferences);
        fallbackContainer.add(objectprefix, eo);
    }

    /**
     * Remove a fallback
     * 
     * @param objectprefix
     *            The prefix to remove the fallback for.
     */
    public void removeFallback(String objectprefix) {
        fallbackContainer.remove(objectprefix);
    }

    /**
     * Stop Exporting an object
     * 
     * @param objectpath
     *            The objectpath to stop exporting.
     */
    public void unExportObject(String objectpath) {
        synchronized (getExportedObjects()) {
            getExportedObjects().remove(objectpath);
            getObjectTree().remove(objectpath);
        }
    }

    /**
     * Send a message to the DBus daemon.
     * @param _message
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
            List<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(key);
            if (null == v) {
                v = new ArrayList<>();
                v.add(handler);
                getHandledSignals().put(key, v);
            } else {
                v.add(handler);
            }
        }
    }
    
    /**
     * Disconnect from the Bus.
     */
    public synchronized void disconnect() {

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

        try {
            // try to wait for all pending tasks.
            workerThreadPool.shutdown();
            workerThreadPool.awaitTermination(10, TimeUnit.SECONDS); // 10 seconds should be enough, otherwise fail
            
        } catch (InterruptedException _ex) {
            logger.error("Interrupted while waiting for worker threads to be terminated.", _ex);
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
                transport.disconnect();
                transport = null;
            }
        } catch (IOException exIo) {
            logger.debug("Exception while disconnecting transport.", exIo);
        }

        // stop all the workers
        if (!workerThreadPool.isTerminated()) { // try forceful shutdown
            workerThreadPool.shutdownNow();
        }
        
    }

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
        Class<?>[] types = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            types[i] = parameters[i].getClass();
        }
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
        Class<?>[] types = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            types[i] = parameters[i].getClass();
        }
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
        workerThreadPool.execute(r);
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
        logger.debug("Handling incoming signal: ", _signal);
        List<DBusSigHandler<? extends DBusSignal>> v = new ArrayList<>();
        synchronized (getHandledSignals()) {
            List<DBusSigHandler<? extends DBusSignal>> t;
            t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), null, null));
            if (null != t) {
                v.addAll(t);
            }
            t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), _signal.getPath(), null));
            if (null != t) {
                v.addAll(t);
            }
            t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), null, _signal.getSource()));
            if (null != t) {
                v.addAll(t);
            }
            t = getHandledSignals().get(new SignalTuple(_signal.getInterface(), _signal.getName(), _signal.getPath(), _signal.getSource()));
            if (null != t) {
                v.addAll(t);
            }
        }
        if (0 == v.size()) {
            return;
        }

        final AbstractConnection conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : v) {
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
                        logger.warn("Exception while running signal handler '{}' for signal '{}': {}", h, _signal, _ex);
                        handleException(conn, _signal, new DBusExecutionException("Error handling signal " + _signal.getInterface()
                                + "." + _signal.getName() + ": " + _ex.getMessage()));
                    }
                }
            };
            if (_useThreadPool) {
                workerThreadPool.execute(command);
            } else {
                command.run();
            }
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
                workerThreadPool.execute(command);
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
                workerThreadPool.execute(r);
            }

        } else
            try {
                sendMessage(new Error(mr, new DBusExecutionException(
                        "Spurious reply. No message with the given serial id was awaiting a reply.")));
            } catch (DBusException exDe) {
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
     * @throws ParseException
     *             on error
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

    protected Map<SignalTuple, List<DBusSigHandler<? extends DBusSignal>>> getHandledSignals() {
        return handledSignals;
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
}
