/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.connection;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.freedesktop.DBus;
import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.CallbackHandler;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Error;
import org.freedesktop.dbus.ExportedObject;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.MethodReturn;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.ObjectTree;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.SignalTuple;
import org.freedesktop.dbus.Transport;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalDBusException;
import org.freedesktop.dbus.exceptions.FatalException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.threads.NameableThreadFactory;

/** Handles a connection to DBus.
 */
public abstract class AbstractConnection implements Closeable {
    private final Logger        logger = LoggerFactory.getLogger(getClass());

    public static final boolean FLOAT_SUPPORT;
    static {
        FLOAT_SUPPORT = (null != System.getenv("DBUS_JAVA_FLOATS"));
    }

    public static final String                                             BUSNAME_REGEX    =
            "^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$";
    public static final String                                             CONNID_REGEX     = "^:[0-9]*\\.[0-9]*$";
    public static final String                                             OBJECT_REGEX     =
            "^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$";
    public static final Pattern                                            DOLLAR_PATTERN   = Pattern.compile("[$]");

    public static final int                                                MAX_ARRAY_LENGTH = 67108864;
    public static final int                                                MAX_NAME_LENGTH  = 255;

    /**
     * Timeout in us on checking the BUS for incoming messages and sending outgoing messages
     */
    protected static final int                                             TIMEOUT          = 100000;
    /** Initial size of the pending calls map */
    static final byte                                                      THREADCOUNT      = 4;

    private ObjectTree                                                     objectTree;
    private GlobalHandler                                                  globalHandlerReference;
    
    protected Map<String, ExportedObject>                                    exportedObjects;
    protected Map<DBusInterface, RemoteObject>                               importedObjects;

    protected Map<SignalTuple, Vector<DBusSigHandler<? extends DBusSignal>>> handledSignals;
    protected Map<Long, MethodCall>                                          pendingCalls;

    private PendingCallbackManager                                         callbackManager;

    private ExecutorService                                                workerThreadPool;
    private FallbackContainer                                              fallbackcontainer;
    private volatile boolean                                               run;
    
    private LinkedBlockingQueue<Message>                                   outgoingQueue;
    LinkedList<Error>                                                      pendingErrors;

    private ConnectionThread                                               connectionThread;
    private SenderThread                                                   senderThread;
    private Transport                                                      transport;

    private boolean                                                        weakreferences   = false;
    private boolean                                                        connected        = false;

    private static final Map<Thread, DBusCallInfo>                         INFOMAP          = new ConcurrentHashMap<>();

    private BusAddress                                                     busAddress;
    
    protected class FallbackContainer {
        private final Logger                  logger    = LoggerFactory.getLogger(getClass());
        private Map<String[], ExportedObject> fallbacks = new HashMap<>();

        public synchronized void add(String path, ExportedObject eo) {
            logger.debug("Adding fallback on " + path + " of " + eo);
            fallbacks.put(path.split("/"), eo);
        }

        public synchronized void remove(String path) {
            logger.debug("Removing fallback on " + path);
            fallbacks.remove(path.split("/"));
        }

        public synchronized ExportedObject get(String path) {
            int best = 0;
            int i = 0;
            ExportedObject bestobject = null;
            String[] pathel = path.split("/");
            for (String[] fbpath : fallbacks.keySet()) {
                logger.trace("Trying fallback path " + Arrays.deepToString(fbpath) + " to match " + Arrays.deepToString(pathel));
                for (i = 0; i < pathel.length && i < fbpath.length; i++) {
                    if (!pathel[i].equals(fbpath[i])) {
                        break;
                    }
                }
                if (i > 0 && i == fbpath.length && i > best) {
                    bestobject = fallbacks.get(fbpath);
                }
                logger.trace("Matches " + i + " bestobject now " + bestobject);
            }

            logger.debug("Found fallback for " + path + " of " + bestobject);
            return bestobject;
        }
    }

    protected class ConnectionThread extends Thread {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        public ConnectionThread() {
            setName("DBusConnection");
        }

        @Override
        public void run() {
            try {
                Message m = null;
                while (run) {
                    m = null;

                    // read from the wire
                    try {
                        // this blocks on outgoing being non-empty or a message being available.
                        m = readIncoming();
                        if (m != null) {
                            logger.trace("Got Incoming Message: " + m);
                            synchronized (this) {
                                notifyAll();
                            }

                            if (m instanceof DBusSignal) {
                                handleMessage((DBusSignal) m);
                            } else if (m instanceof MethodCall) {
                                handleMessage((MethodCall) m);
                            } else if (m instanceof MethodReturn) {
                                handleMessage((MethodReturn) m);
                            } else if (m instanceof Error) {
                                handleMessage((Error) m);
                            }

                            m = null;
                        }
                    } catch (Exception e) {
                        logger.error("Exception in connection thread.", e);
                        if (e instanceof FatalException) {
                            disconnect();
                        }
                    }

                }
                synchronized (this) {
                    notifyAll();
                }
            } catch (Exception e) {
                logger.debug("", e);
            }
        }
    }

    private class GlobalHandler implements org.freedesktop.DBus.Peer, org.freedesktop.DBus.Introspectable {
        private String objectpath;

        GlobalHandler() {
            this.objectpath = null;
        }

        GlobalHandler(String _objectpath) {
            this.objectpath = _objectpath;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public void Ping() {
            return;
        }

        @Override
        public String Introspect() {
            String intro = objectTree.Introspect(objectpath);
            if (null == intro) {
                ExportedObject eo = fallbackcontainer.get(objectpath);
                if (null != eo) {
                    intro = eo.getIntrospectiondata();
                }
            }
            if (null == intro) {
                throw new DBus.Error.UnknownObject("Introspecting on non-existant object");
            } else {
                return "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" " + "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n" + intro;
            }
        }

        @Override
        public String getObjectPath() {
            return objectpath;
        }
    }

    private class SenderThread extends Thread {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        private boolean terminate;
        
        SenderThread() {
            setName("DBUS Sender Thread");
        }

        public void terminate() {
            terminate = true;
            interrupt();
        }
        
        @Override
        public void run() {
            Message m = null;

            logger.trace("Monitoring outbound queue");
            // block on the outbound queue and send from it
            while (!terminate) {
                try {
                    m = outgoingQueue.take();
                    if (m != null) {
                        sendMessage(m);
                        m = null;
                    }
                } catch (InterruptedException _ex) {
                    logger.warn("Interrupted while waiting for message to send", _ex);
                }
            }

            logger.debug("Flushing outbound queue and quitting");
            // flush the outbound queue before disconnect.
            while (!outgoingQueue.isEmpty()) {
                Message poll = outgoingQueue.poll();
                if (poll != null) {
                    sendMessage(outgoingQueue.poll());
                } else {
                    break;
                }
            }
        }
    }

    

    protected AbstractConnection(String address) throws DBusException {
        exportedObjects = new HashMap<>();
        importedObjects = new HashMap<>();
        globalHandlerReference = new GlobalHandler();
        synchronized (exportedObjects) {
            exportedObjects.put(null, new ExportedObject(globalHandlerReference, weakreferences));
        }
        handledSignals = new HashMap<>();
        pendingCalls = new ConcurrentSkipListMap<>(); // TODO: EfficentMap used insertion order, is natural order also suitable?
        outgoingQueue = new LinkedBlockingQueue<>();
        
        callbackManager = new PendingCallbackManager();
        
        pendingErrors = new LinkedList<>();
        workerThreadPool = Executors.newFixedThreadPool(THREADCOUNT, new NameableThreadFactory("DbusWorkerThreads", false));
        
        objectTree = new ObjectTree();
        fallbackcontainer = new FallbackContainer();
        run = true;
        
        try {
            busAddress = new BusAddress(address);
            transport = new Transport(busAddress, AbstractConnection.TIMEOUT);
            connected = true;
        } catch (IOException | DBusException ioe) {
            logger.debug("Error creating transport", ioe);
            disconnect();
            throw new DBusException("Failed to connect to bus " + ioe.getMessage());
        }
    }

    protected void listen() {
        // start listening
        connectionThread = new ConnectionThread();
        connectionThread.start();
        senderThread = new SenderThread();
        senderThread.start();
    }

    /**
    * Change the number of worker threads to receive method calls and handle signals.
    * Default is 4 threads
    * @param newcount The new number of worker Threads to use.
    */
    public void changeThreadCount(byte newcount) {
        if (newcount != THREADCOUNT) {
            List<Runnable> remainingTasks = workerThreadPool.shutdownNow(); // kill previous threadpool
            workerThreadPool = Executors.newFixedThreadPool(newcount, new NameableThreadFactory("DbusWorkerThreads", false));
            // re-schedule previously waiting tasks
            for (Runnable runnable : remainingTasks) {
                workerThreadPool.execute(runnable);    
            }            
        }     
    }

    private void addRunnable(Runnable r) {
        workerThreadPool.execute(r);
    }

    public String getExportedObject(DBusInterface i) throws DBusException {
        synchronized (exportedObjects) {
            for (String s : exportedObjects.keySet()) {
                if (i.equals(exportedObjects.get(s).getObject().get())) {
                    return s;
                }
            }
        }

        String s = importedObjects.get(i).getObjectPath();
        if (null != s) {
            return s;
        }

        throw new DBusException("Not an object exported or imported by this connection");
    }

    public abstract DBusInterface getExportedObject(String source, String path) throws DBusException;

    /**
    * Returns a structure with information on the current method call.
    * @return the DBusCallInfo for this method call, or null if we are not in a method call.
    */
    public static DBusCallInfo getCallInfo() {
        return INFOMAP.get(Thread.currentThread());
    }

    /**
    * If set to true the bus will not hold a strong reference to exported objects.
    * If they go out of scope they will automatically be unexported from the bus.
    * The default is to hold a strong reference, which means objects must be
    * explicitly unexported before they will be garbage collected.
    *
    * @param _weakreferences reference
    */
    public void setWeakReferences(boolean _weakreferences) {
        this.weakreferences = _weakreferences;
    }

    /**
    * Export an object so that its methods can be called on DBus.
    * @param objectpath The path to the object we are exposing. MUST be in slash-notation, like "/org/freedesktop/Local",
    * and SHOULD end with a capitalised term. Only one object may be exposed on each path at any one time, but an object
    * may be exposed on several paths at once.
    * @param object The object to export.
    * @throws DBusException If the objectpath is already exporting an object.
    *  or if objectpath is incorrectly formatted,
    */
    public void exportObject(String objectpath, DBusInterface object) throws DBusException {
        if (null == objectpath || "".equals(objectpath)) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        synchronized (exportedObjects) {
            if (null != exportedObjects.get(objectpath)) {
                throw new DBusException("Object already exported");
            }
            ExportedObject eo = new ExportedObject(object, weakreferences);
            exportedObjects.put(objectpath, eo);
            objectTree.add(objectpath, eo, eo.getIntrospectiondata());
        }
    }

    /**
    * Export an object as a fallback object.
    * This object will have it's methods invoked for all paths starting
    * with this object path.
    * @param objectprefix The path below which the fallback handles calls.
    * MUST be in slash-notation, like "/org/freedesktop/Local",
    * @param object The object to export.
    * @throws DBusException If the objectpath is incorrectly formatted,
    */
    public void addFallback(String objectprefix, DBusInterface object) throws DBusException {
        if (null == objectprefix || "".equals(objectprefix)) {
            throw new DBusException("Must Specify an Object Path");
        }
        if (!objectprefix.matches(OBJECT_REGEX) || objectprefix.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectprefix);
        }
        ExportedObject eo = new ExportedObject(object, weakreferences);
        fallbackcontainer.add(objectprefix, eo);
    }

    /**
    * Remove a fallback
    * @param objectprefix The prefix to remove the fallback for.
    */
    public void removeFallback(String objectprefix) {
        fallbackcontainer.remove(objectprefix);
    }

    /**
    * Stop Exporting an object
    * @param objectpath The objectpath to stop exporting.
    */
    public void unExportObject(String objectpath) {
        synchronized (exportedObjects) {
            exportedObjects.remove(objectpath);
            objectTree.remove(objectpath);
        }
    }
    
    /**
    * Send a signal.
    * @param signal The signal to send.
    */
    public void sendSignal(DBusSignal signal) {
        queueOutgoing(signal);
    }

    public void queueOutgoing(Message m) {
        outgoingQueue.add(m);       
    }

    /**
    * Remove a Signal Handler.
    * Stops listening for this signal.
    *
    * @param <T> class extending {@link DBusSignal}
    * @param type The signal to watch for.
    * @param handler the handler
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        removeSigHandler(new DBusMatchRule(type), handler);
    }

    /**
    * Remove a Signal Handler.
    * Stops listening for this signal.
    *
    * @param <T> class extending {@link DBusSignal}
    * @param type The signal to watch for.
    * @param object The object emitting the signal.
    * @param handler the handler
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        String objectpath = importedObjects.get(object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        removeSigHandler(new DBusMatchRule(type, null, objectpath), handler);
    }

    protected abstract <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

    /**
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type and name.
    *
    * @param <T> class extending {@link DBusSignal}
    * @param type The signal to watch for.
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
    public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        addSigHandler(new DBusMatchRule(type), handler);
    }

    /**
    * Add a Signal Handler.
    * Adds a signal handler to call when a signal is received which matches the specified type, name and object.
    *
    * @param <T> class extending {@link DBusSignal}
    * @param type The signal to watch for.
    * @param object The object from which the signal will be emitted
    * @param handler The handler to call when a signal is received.
    * @throws DBusException If listening for the signal on the bus failed.
    * @throws ClassCastException If type is not a sub-type of DBusSignal.
    */
    public <T extends DBusSignal> void addSigHandler(Class<T> type, DBusInterface object, DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        String objectpath = importedObjects.get(object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        addSigHandler(new DBusMatchRule(type, null, objectpath), handler);
    }

    protected abstract <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException;

    protected <T extends DBusSignal> void addSigHandlerWithoutMatch(Class<? extends DBusSignal> signal, DBusSigHandler<T> handler) throws DBusException {
        DBusMatchRule rule = new DBusMatchRule(signal);
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        synchronized (handledSignals) {
            Vector<DBusSigHandler<? extends DBusSignal>> v = handledSignals.get(key);
            if (null == v) {
                v = new Vector<>();
                v.add(handler);
                handledSignals.put(key, v);
            } else {
                v.add(handler);
            }
        }
    }

    /**
    * Disconnect from the Bus.
    */
    public void disconnect() {        
        connected = false;
        logger.debug("Sending disconnected signal");
        try {
            handleMessage(new org.freedesktop.DBus.Local.Disconnected("/"));
        } catch (Exception ex) {
            logger.debug("Exception while disconnecting", ex);
        }

        // stop sending messages
        senderThread.terminate();
        
        logger.debug("Disconnecting Abstract Connection");
        
        try {
            // try to wait for all pending tasks.
            workerThreadPool.shutdown();
            workerThreadPool.awaitTermination(10, TimeUnit.SECONDS); // 10 seconds should be enough, otherwise fail
        } catch (InterruptedException _ex) {
            logger.error("Interrupted while waiting for worker threads to be terminated.", _ex);
        }

        // stop the main thread
        run = false;

        // disconnect from the transport layer
        try {
            if (null != transport) {
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
    * Return any DBus error which has been received.
    * @return A DBusExecutionException, or null if no error is pending.
    */
    public DBusExecutionException getError() {
        synchronized (pendingErrors) {
            if (pendingErrors.size() == 0) {
                return null;
            } else {
                return pendingErrors.removeFirst().getException();
            }
        }
    }

    /**
    * Call a method asynchronously and set a callback.
    * This handler will be called in a separate thread.
    * @param <A> whatever
    * @param object The remote object on which to call the method.
    * @param m The name of the method on the interface to call.
    * @param callback The callback handler.
    * @param parameters The parameters to call the method with.
    */
    public <A> void callWithCallback(DBusInterface object, String m, CallbackHandler<A> callback, Object... parameters) {
        logger.trace("callWithCallback(" + object + "," + m + ", " + callback);
        Class<?>[] types = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            types[i] = parameters[i].getClass();
        }
        RemoteObject ro = importedObjects.get(object);

        try {
            Method me;
            if (null == ro.getInterface()) {
                me = object.getClass().getMethod(m, types);
            } else {
                me = ro.getInterface().getMethod(m, types);
            }
            RemoteInvocationHandler.executeRemoteMethod(ro, me, this, RemoteInvocationHandler.CALL_TYPE_CALLBACK, callback, parameters);
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
    * @param object The remote object on which to call the method.
    * @param m The name of the method on the interface to call.
    * @param parameters The parameters to call the method with.
    * @return A handle to the call.
    */
    public DBusAsyncReply<?> callMethodAsync(DBusInterface object, String m, Object... parameters) {
        Class<?>[] types = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            types[i] = parameters[i].getClass();
        }
        RemoteObject ro = importedObjects.get(object);

        try {
            Method me;
            if (null == ro.getInterface()) {
                me = object.getClass().getMethod(m, types);
            } else {
                me = ro.getInterface().getMethod(m, types);
            }
            return (DBusAsyncReply<?>) RemoteInvocationHandler.executeRemoteMethod(ro, me, this, RemoteInvocationHandler.CALL_TYPE_ASYNC, null, parameters);
        } catch (DBusExecutionException exDee) {
            logger.debug("", exDee);
            throw exDee;
        } catch (Exception e) {
            logger.debug("", e);
            throw new DBusExecutionException(e.getMessage());
        }
    }

    private void handleMessage(final MethodCall m) throws DBusException {
        logger.debug("Handling incoming method call: " + m);

        ExportedObject eo = null;
        Method meth = null;
        Object o = null;

        if (null == m.getInterface() || m.getInterface().equals("org.freedesktop.DBus.Peer") || m.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            synchronized (exportedObjects) {
                eo = exportedObjects.get(null);
            }
            if (null != eo && null == eo.getObject().get()) {
                unExportObject(null);
                eo = null;
            }
            if (null != eo) {
                meth = eo.getMethods().get(new MethodTuple(m.getName(), m.getSig()));
            }
            if (null != meth) {
                o = new GlobalHandler(m.getPath());
            } else {
                eo = null;
            }
        }
        if (null == o) {
            // now check for specific exported functions

            synchronized (exportedObjects) {
                eo = exportedObjects.get(m.getPath());
            }
            if (null != eo && null == eo.getObject().get()) {
                logger.info("Unexporting " + m.getPath() + " implicitly");
                unExportObject(m.getPath());
                eo = null;
            }

            if (null == eo) {
                eo = fallbackcontainer.get(m.getPath());
            }

            if (null == eo) {
                queueOutgoing(new Error(m, new DBus.Error.UnknownObject(m.getPath() + " is not an object provided by this process.")));
                return;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Searching for method " + m.getName() + " with signature " + m.getSig());
                logger.trace("List of methods on " + eo + ":");
                for (MethodTuple mt : eo.getMethods().keySet()) {
                    logger.trace("   " + mt + " => " + eo.getMethods().get(mt));
                }
            }
            meth = eo.getMethods().get(new MethodTuple(m.getName(), m.getSig()));
            if (null == meth) {
                queueOutgoing(new Error(m, new DBus.Error.UnknownMethod(MessageFormat.format("The method `{0}.{1}' does not exist on this object.", m.getInterface(), m.getName()))));
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

        logger.trace("Adding Runnable for method " + meth);
        addRunnable(new Runnable() {
            private boolean run = false;

            @Override
            public synchronized void run() {
                if (run) {
                    return;
                }
                run = true;
                logger.debug("Running method " + me + " for remote call");

                try {
                    Type[] ts = me.getGenericParameterTypes();
                    m.setArgs(Marshalling.deSerializeParameters(m.getParameters(), ts, conn));
                    logger.trace("Deserialised " + Arrays.deepToString(m.getParameters()) + " to types " + Arrays.deepToString(ts));
                } catch (Exception e) {
                    logger.debug("", e);
                    handleException(conn, m, new DBus.Error.UnknownMethod("Failure in de-serializing message: " + e));
                    return;
                }

                try {
                    INFOMAP.put(Thread.currentThread(), info);
                    Object result;
                    try {
                        logger.trace("Invoking Method: " + me + " on " + ob + " with parameters " + Arrays.deepToString(m.getParameters()));
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
                        conn.queueOutgoing(reply);
                    }
                } catch (DBusExecutionException exDee) {
                    logger.debug("", exDee);
                    handleException(conn, m, exDee);
                } catch (Throwable e) {
                    logger.debug("", e);
                    handleException(conn, m, new DBusExecutionException(MessageFormat.format("Error Executing Method {0}.{1}: {2}", m.getInterface(), m.getName(), e.getMessage())));
                }
            }
        });
    }

    protected void handleException(AbstractConnection dbusConnection, Message methodOrSignal, DBusExecutionException exception) {
        if (dbusConnection == null) {
            throw new NullPointerException("DBusConnection cannot be null");
        }
        try {
            dbusConnection.queueOutgoing(new Error(methodOrSignal, exception));
        } catch (DBusException ex) {
            logger.warn("Exception caught while processing previous error.", ex);
        }
    }

    @SuppressWarnings({
            "unchecked"
    })
    private void handleMessage(final DBusSignal s) {
        logger.debug("Handling incoming signal: " + s);
        List<DBusSigHandler<? extends DBusSignal>> v = new ArrayList<>();
        synchronized (handledSignals) {
            Vector<DBusSigHandler<? extends DBusSignal>> t;
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), null, null));
            if (null != t) {
                v.addAll(t);
            }
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), s.getPath(), null));
            if (null != t) {
                v.addAll(t);
            }
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), null, s.getSource()));
            if (null != t) {
                v.addAll(t);
            }
            t = handledSignals.get(new SignalTuple(s.getInterface(), s.getName(), s.getPath(), s.getSource()));
            if (null != t) {
                v.addAll(t);
            }
        }
        if (0 == v.size()) {
            return;
        }
        
        final AbstractConnection conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : v) {
            logger.trace("Adding Runnable for signal " + s + " with handler " + h);
            addRunnable(new Runnable() {
                private boolean run = false;

                @Override
                public synchronized void run() {
                    if (run) {
                        return;
                    }
                    run = true;
                    try {
                        DBusSignal rs;
                        if (s instanceof DBusSignal.internalsig || s.getClass().equals(DBusSignal.class)) {
                            rs = s.createReal(conn);
                        } else {
                            rs = s;
                        }
                        ((DBusSigHandler<DBusSignal>) h).handle(rs);
                    } catch (DBusException exDe) {
                        logger.debug("", exDe);
                        handleException(conn, s, new DBusExecutionException("Error handling signal " + s.getInterface() + "." + s.getName() + ": " + exDe.getMessage()));
                    }
                }
            });
        }
    }

    public void queueCallback(MethodCall _call, Method _method, CallbackHandler<?> _callback) {
        callbackManager.queueCallback(_call, _method, _callback, this);
    }
    
    private void handleMessage(final Error err) {
        logger.debug("Handling incoming error: " + err);
        MethodCall m = null;
        if (null == pendingCalls) {
            return;
        }
        synchronized (pendingCalls) {
            if (pendingCalls.containsKey(err.getReplySerial())) {
                m = pendingCalls.remove(err.getReplySerial());
            }
        }
        if (null != m) {
            m.setReply(err);
            CallbackHandler<?> cbh = null;
            cbh = callbackManager.removeCallback(m);
            logger.trace(cbh + " = pendingCallbacks.remove(" + m + ")");
            
            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<?> fcbh = cbh;
                logger.trace("Adding Error Runnable with callback handler " + fcbh);
                addRunnable(new Runnable() {
                    private boolean run = false;

                    @Override
                    public synchronized void run() {
                        if (run) {
                            return;
                        }
                        run = true;
                        try {
                            logger.trace("Running Error Callback for " + err);
                            DBusCallInfo info = new DBusCallInfo(err);
                                INFOMAP.put(Thread.currentThread(), info);

                            fcbh.handleError(err.getException());
                            INFOMAP.remove(Thread.currentThread());

                        } catch (Exception e) {
                            logger.debug("Exception while running error callback.", e);
                        }
                    }
                });
            }

        } else {
            synchronized (pendingErrors) {
                pendingErrors.addLast(err);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(final MethodReturn mr)
    {
       logger.debug("Handling incoming method return: "+mr);
       MethodCall m = null;
       if (null == pendingCalls) return;
       synchronized (pendingCalls) {
          if (pendingCalls.containsKey(mr.getReplySerial()))
             m = pendingCalls.remove(mr.getReplySerial());
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
             logger.trace("Adding Runnable for method {} with callback handler {}", fcbh, fasr != null ? fasr.getMethod() : null);
             addRunnable(new Runnable() {
                private boolean run = false;
                @Override
                public synchronized void run()
                {
                   if (run) return;
                   run = true;
                   try {
                      logger.trace("Running Callback for "+mr);
                      DBusCallInfo info = new DBusCallInfo(mr);
                      INFOMAP.put(Thread.currentThread(), info);
                      Object convertRV = RemoteInvocationHandler.convertRV(mr.getSig(), mr.getParameters(), fasr.getMethod(), fasr.getConnection());
                      fcbh.handle(convertRV);
                      INFOMAP.remove(Thread.currentThread());

                   } catch (Exception e) {
                      logger.debug("Exception while running callback.", e);
                   }
                }
             });
          }

       } else
          try {
             queueOutgoing(new Error(mr, new DBusExecutionException("Spurious reply. No message with the given serial id was awaiting a reply.")));
          } catch (DBusException exDe) {}
    }

    protected void sendMessage(Message m) {
        try {
            if (!connected) {
                throw new NotConnected("Disconnected");
            }
            if (m instanceof DBusSignal) {
                ((DBusSignal) m).appendbody(this);
            }

            if (m instanceof MethodCall) {
                if (0 == (m.getFlags() & Message.Flags.NO_REPLY_EXPECTED)) {
                    if (null == pendingCalls) {
                        ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.Disconnected", 0, "s", "Disconnected"));
                    } else {
                        synchronized (pendingCalls) {
                            pendingCalls.put(m.getSerial(), (MethodCall) m);
                        }
                    }
                }
            }

            transport.writeMessage(m);

        } catch (Exception e) {
            logger.debug("Exception while sending message.", e);
            if (m instanceof MethodCall && e instanceof NotConnected) {
                try {
                    ((MethodCall) m).setReply(new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.Disconnected", 0, "s", "Disconnected"));
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
                    logger.info("Setting reply to " + m + " as an error");
                    ((MethodCall) m).setReply(new Error(m, new DBusExecutionException("Message Failed to Send: " + e.getMessage())));
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

    private Message readIncoming() throws DBusException {
        if (!connected) {
            throw new NotConnected("No transport present");
        }
        Message m = null;
        try {
            m = transport.readMessage();
        } catch (IOException exIo) {
            if (!run && (exIo instanceof EOFException)) { // EOF is expected when connection is shutdown
                return null;
            }
            throw new FatalDBusException(exIo.getMessage());
        }
        return m;
    }

    /**
    * Returns the address this connection is connected to.
    * @return new {@link BusAddress} object
    * @throws ParseException on error
    */
    public BusAddress getAddress() {
        return busAddress;
    }

    public boolean isConnected() {
        return connected;
    }
}
