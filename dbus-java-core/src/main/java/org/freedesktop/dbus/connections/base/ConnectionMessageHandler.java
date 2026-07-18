package org.freedesktop.dbus.connections.base;

import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.ConnectionConfig;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusMonitorHandler;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.messages.*;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.propertyref.PropertyRef;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.DBusObjects;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

/**
 * Abstract class containing most methods to handle/react to a message received on a connection. <br>
 * Part of the {@link AbstractConnectionBase} &rarr;  {@link ConnectionMethodInvocation}
 * &rarr; {@link DBusBoundPropertyHandler} &rarr; {@link ConnectionMessageHandler} &rarr; {@link AbstractConnection} hierarchy.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-23
 */
public abstract sealed class ConnectionMessageHandler extends DBusBoundPropertyHandler permits AbstractConnection {

    private static final Method GET_MANAGED_OBJECTS_METHOD = getManagedObjectsMethod();

    /** When set, this connection acts as a monitor and raw messages are delivered to this handler. */
    private volatile DBusMonitorHandler monitorHandler;

    protected ConnectionMessageHandler(ConnectionConfig _conCfg, TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_conCfg, _transportConfig, _rsCfg);
    }

    /**
     * Sets (or clears with {@code null}) the monitor handler. When set, this connection is treated as a
     * monitor connection: incoming messages are delivered to the handler instead of the normal dispatch.
     *
     * @param _monitorHandler monitor handler or {@code null} to leave monitor mode
     */
    protected void setMonitorHandler(DBusMonitorHandler _monitorHandler) {
        monitorHandler = _monitorHandler;
    }

    /**
     * @return true if this connection currently is a monitor connection
     */
    protected boolean isMonitor() {
        return monitorHandler != null;
    }

    @Override
    protected void handleException(Message _methodOrSignal, DBusExecutionException _exception) {
        try {
            sendMessage(getMessageFactory().createError(_methodOrSignal, _exception));
        } catch (DBusException _ex) {
            getLogger().warn("Exception caught while processing previous error.", _ex);
        }
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
        getLogger().debug("Handling incoming signal: {}", _signal);

        List<DBusSigHandler<? extends DBusSignal>> handlers = new ArrayList<>();
        List<DBusSigHandler<DBusSignal>> genericHandlers = new ArrayList<>();

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<? extends DBusSignal>>> e : getHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal)) {
                handlers.addAll(e.getValue());
            }
        }

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>> e : getGenericHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal)) {
                genericHandlers.addAll(e.getValue());
            }
        }

        if (handlers.isEmpty() && genericHandlers.isEmpty()) {
            return;
        }

        final AbstractConnectionBase conn = this;
        for (final DBusSigHandler<? extends DBusSignal> h : handlers) {
            getLogger().trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = () -> {
                try {
                    DBusSignal rs;
                    if (_signal.getClass().equals(DBusSignal.class)) {
                        rs = _signal.createReal(conn);
                    } else {
                        rs = _signal;
                    }
                    if (rs == null) {
                        if (getConnectionConfig().getUnknownSignalHandler() != null) {
                            getConnectionConfig().getUnknownSignalHandler().accept(_signal);
                        }
                        return;
                    }
                    ((DBusSigHandler<DBusSignal>) h).handle(rs);
                } catch (DBusException _ex) {
                    getLogger().warn("Exception while running signal handler '{}' for signal '{}':", h, _signal, _ex);
                    handleException(_signal, new DBusExecutionException("Error handling signal " + _signal.getInterface()
                            + "." + _signal.getName() + ": " + _ex.getMessage(), _ex));
                }
            };
            if (_useThreadPool) {
                getReceivingService().execSignalHandler(command);
            } else {
                command.run();
            }
        }

        for (final DBusSigHandler<DBusSignal> h : genericHandlers) {
            getLogger().trace("Adding Runnable for signal {} with handler {}",  _signal, h);
            Runnable command = () -> h.handle(_signal);
            if (_useThreadPool) {
                getReceivingService().execSignalHandler(command);
            } else {
                command.run();
            }
        }
    }

    protected void handleMessage(final Error _err) {
        getLogger().debug("Handling incoming error: {}", _err);
        MethodCall m = null;
        if (getPendingCalls() == null) {
            return;
        }
        synchronized (getPendingCalls()) {
            if (getPendingCalls().containsKey(_err.getReplySerial())) {
                m = getPendingCalls().remove(_err.getReplySerial());
            }
        }
        if (m != null) {
            m.setReply(_err);
            CallbackHandler<?> cbh;
            cbh = getCallbackManager().removeCallback(m);
            getLogger().trace("{} = pendingCallbacks.remove({})", cbh, m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<?> fcbh = cbh;
                getLogger().trace("Adding Error Runnable with callback handler {}", fcbh);
                Runnable command = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            getLogger().trace("Running Error Callback for {}", _err);
                            DBusCallInfo info = new DBusCallInfo(_err);
                            getInfoMap().put(Thread.currentThread(), info);

                            try {
                                fcbh.handleError(_err.getException());
                            } finally {
                                getInfoMap().remove(Thread.currentThread());
                            }

                        } catch (Exception _ex) {
                            getLogger().debug("Exception while running error callback.", _ex);
                        }
                    }
                };
                getReceivingService().execErrorHandler(command);
            }

        } else {
            addPendingError(_err);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleMessage(final MethodReturn _mr) {
        getLogger().debug("Handling incoming method return: {}", _mr);
        MethodCall m = null;

        if (null == getPendingCalls()) {
            return;
        }

        synchronized (getPendingCalls()) {
            if (getPendingCalls().containsKey(_mr.getReplySerial())) {
                m = getPendingCalls().remove(_mr.getReplySerial());
            }
        }

        if (null != m) {
            m.setReply(_mr);
            _mr.setCall(m);
            @SuppressWarnings("rawtypes")
            CallbackHandler cbh = getCallbackManager().getCallback(m);
            DBusAsyncReply<?> asr = getCallbackManager().getCallbackReply(m);
            getCallbackManager().removeCallback(m);

            // queue callback for execution
            if (null != cbh) {
                final CallbackHandler<Object> fcbh = cbh;
                final DBusAsyncReply<?> fasr = asr;
                if (fasr == null) {
                    getLogger().debug("Cannot add runnable for method, given method callback was null");
                    return;
                }
                getLogger().trace("Adding Runnable for method {} with callback handler {}", fcbh, fasr.getMethod());
                Runnable r = new Runnable() {

                    @Override
                    public synchronized void run() {
                        try {
                            getLogger().trace("Running Callback for {}", _mr);
                            DBusCallInfo info = new DBusCallInfo(_mr);
                            getInfoMap().put(Thread.currentThread(), info);
                            try {
                                Object convertRV = RemoteInvocationHandler.convertRV(_mr.getParameters(), fasr.getMethod(),
                                        fasr.getConnection());
                                fcbh.handle(convertRV);
                            } finally {
                                getInfoMap().remove(Thread.currentThread());
                            }

                        } catch (Exception _ex) {
                            getLogger().debug("Exception while running callback.", _ex);
                        }
                    }
                };
                getReceivingService().execMethodReturnHandler(r);
            }

        } else {
            try {
                sendMessage(getMessageFactory().createError(_mr, new DBusExecutionException(
                        "Spurious reply. No message with the given serial id was awaiting a reply.")));
            } catch (DBusException _exDe) {
                getLogger().trace("Could not send error message", _exDe);
            }
        }
    }

    /**
     * Handle received message from DBus.
     * @param _message
     * @throws DBusException
     */
    void handleMessage(Message _message) throws DBusException {
        DBusMonitorHandler monitor = monitorHandler;
        if (monitor != null && !isReplyToPendingCall(_message)) {
            try {
                monitor.handle(_message);
            } catch (RuntimeException _ex) {
                getLogger().warn("Monitor handler failed for message {}", _message, _ex);
            }
            return;
        }

        if (_message instanceof DBusSignal sig) {
            handleMessage(sig, true);
        } else if (_message instanceof MethodCall mc) {
            handleMessage(mc);
        } else if (_message instanceof MethodReturn mr) {
            handleMessage(mr);
        } else if (_message instanceof Error err) {
            handleMessage(err);
        }
    }

    /**
     * Checks whether the given message is a reply (return or error) to a call this connection is still
     * awaiting. Used so a monitor connection can still complete its own {@code BecomeMonitor} call.
     *
     * @param _message message to check
     * @return true if the message replies to a pending call of this connection
     */
    private boolean isReplyToPendingCall(Message _message) {
        long replySerial;
        if (_message instanceof MethodReturn mr) {
            replySerial = mr.getReplySerial();
        } else if (_message instanceof Error err) {
            replySerial = err.getReplySerial();
        } else {
            return false;
        }
        return getPendingCalls() != null && getPendingCalls().containsKey(replySerial);
    }

    private void handleMessage(final MethodCall _methodCall) throws DBusException {
        getLogger().debug("Handling incoming method call: {}", _methodCall);

        ExportedObject exportObject;
        Method meth = null;
        Object o = null;

        if (null == _methodCall.getInterface() || _methodCall.getInterface().equals("org.freedesktop.DBus.Peer")
                || _methodCall.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            exportObject = doWithExportedObjectsAndReturn(DBusException.class, eos -> eos.get(null));
            if (null != exportObject && null == exportObject.getObject().get()) {
                unExportObject(null);
                exportObject = null;
            }
            if (exportObject != null) {
                meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
            }
            if (meth != null) {
                o = new GlobalHandler(this, _methodCall.getPath());
            }
        }
        if (o == null) {
            // now check for specific exported functions

            exportObject = doWithExportedObjectsAndReturn(DBusException.class, eos -> eos.get(_methodCall.getPath()));
            getLogger().debug("Found exported object: {}", exportObject == null ? "<no object found>" : exportObject);

            if (exportObject != null && exportObject.getObject().get() == null) {
                getLogger().info("Unexporting {} implicitly (object present: {}, reference present: {})", _methodCall.getPath(), exportObject != null, exportObject.getObject().get() == null);
                unExportObject(_methodCall.getPath());
                exportObject = null;
            }

            if (exportObject == null) {
                exportObject = getFallbackContainer().get(_methodCall.getPath());
                getLogger().debug("Found {} in fallback container", exportObject == null ? "no" : exportObject);
            }

            if (exportObject == null) {
                getLogger().debug("No object found for method {}", _methodCall.getPath());
                sendMessage(getMessageFactory().createError(_methodCall,
                    new UnknownObject(_methodCall.getPath() + " is not an object provided by this process.")));
                return;
            }
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Searching for method {}  with signature {}", _methodCall.getName(), _methodCall.getSig());
                getLogger().trace("List of methods on {}: ", exportObject);
                for (MethodTuple mt : exportObject.getMethods().keySet()) {
                    getLogger().trace("   {} => {}", mt, exportObject.getMethods().get(mt));
                }
            }

            // automatic ObjectManager handling (unless the connection is configured for manual handling)
            if (!getConnectionConfig().isManualObjectManager()
                && "GetManagedObjects".equals(_methodCall.getName())
                && (_methodCall.getInterface() == null || "org.freedesktop.DBus.ObjectManager".equals(_methodCall.getInterface()))
                && exportObject.getObject().get() instanceof ObjectManager) {
                handleGetManagedObjects(_methodCall);
                return;
            }

            Object[] params = _methodCall.getParameters();
            switch (handleDBusBoundProperties(exportObject, _methodCall, params)) {
                case HANDLED:
                    return;
                case NO_PROPERTY:
                    rejectUnknownProperty(_methodCall, params);
                    return;
                case NOT_HANDLED:
                default:
                    break;
            }

            if (meth == null) {
                meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
                if (meth == null) {
                    sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                        "The method `%s.%s' does not exist on this object.", _methodCall.getInterface(), _methodCall.getName()))));
                    return;
                }
            }
            o = exportObject.getObject().get();
        }

        if (ExportedObject.isExcluded(meth)) {
            sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                    "The method `%s.%s' is not exported.", _methodCall.getInterface(), _methodCall.getName()))));
            return;
        }

        // now execute it
        queueInvokeMethod(_methodCall, meth, o);
    }

    private static Method getManagedObjectsMethod() {
        try {
            return ObjectManager.class.getMethod("GetManagedObjects");
        } catch (NoSuchMethodException _ex) {
            throw new IllegalStateException("ObjectManager.GetManagedObjects method not found", _ex);
        }
    }

    /**
     * Answers an {@code org.freedesktop.DBus.ObjectManager.GetManagedObjects} call automatically by
     * enumerating the exported sub-tree below the ObjectManager and collecting each object's interfaces
     * and properties.
     *
     * @param _methodCall the GetManagedObjects call
     */
    private void handleGetManagedObjects(final MethodCall _methodCall) {
        getReceivingService().execMethodCallHandler(() -> {
            try {
                Map<DBusPath, Map<String, Map<String, Variant<?>>>> managed = buildManagedObjects(_methodCall.getPath());
                invokedMethodReply(_methodCall, GET_MANAGED_OBJECTS_METHOD, managed);
            } catch (DBusExecutionException _ex) {
                getLogger().debug("Failed to answer GetManagedObjects", _ex);
                handleException(_methodCall, _ex);
            } catch (Exception _ex) {
                getLogger().debug("Failed to build managed objects for {}", _methodCall, _ex);
                handleException(_methodCall,
                    new DBusExecutionException("Error building managed objects: " + _ex.getMessage(), _ex));
            }
        });
    }

    /**
     * Builds the {@code GetManagedObjects} response for the sub-tree below the given root path: every
     * exported object which is a descendant of {@code _rootPath}, mapped to its interfaces and their
     * properties (as {@code Properties.GetAll()} would return them).
     *
     * @param _rootPath object path of the ObjectManager (root of the sub-tree)
     *
     * @return map of object path to interface-to-properties map
     */
    public Map<DBusPath, Map<String, Map<String, Variant<?>>>> buildManagedObjects(String _rootPath) {
        return doWithExportedObjectsAndReturn(RuntimeException.class, eos -> {
            Map<DBusPath, Map<String, Map<String, Variant<?>>>> result = new LinkedHashMap<>();
            for (Entry<String, ExportedObject> e : eos.entrySet()) {
                String path = e.getKey();
                if (path == null || path.equals(_rootPath)) {
                    continue;
                }
                boolean descendant = "/".equals(_rootPath) ? !"/".equals(path) : path.startsWith(_rootPath + "/");
                if (!descendant) {
                    continue;
                }
                result.put(new DBusPath(path), collectManagedInterfaces(e.getValue()));
            }
            return result;
        });
    }

    /**
     * Collects the (non-standard) interfaces of an exported object together with their current property
     * values. Bound properties ({@code @DBusBoundProperty}) are read via their getters; objects
     * implementing {@link Properties} directly are queried via {@code GetAll}.
     *
     * @param _eo exported object
     *
     * @return map of interface name to property map
     */
    protected Map<String, Map<String, Variant<?>>> collectManagedInterfaces(ExportedObject _eo) {
        Map<String, Map<String, Variant<?>>> byInterface = new LinkedHashMap<>();
        DBusInterface obj = _eo.getObject().get();
        if (obj == null) {
            return byInterface;
        }

        for (Class<?> iface : _eo.getImplementedInterfaces()) {
            if (DBusObjects.isStandardInterface(iface)) {
                continue;
            }
            String ifaceName = DBusNamingUtil.getInterfaceName(iface);
            Map<String, Variant<?>> props = new LinkedHashMap<>();

            // read bound properties declared on this interface
            for (Entry<PropertyRef, Method> pe : _eo.getPropertyMethods().entrySet()) {
                Method getter = pe.getValue();
                if (pe.getKey().getAccess() == Access.READ && getter.getDeclaringClass() == iface) {
                    try {
                        Object value = getter.invoke(obj);
                        if (value != null) {
                            props.put(pe.getKey().getName(), toVariant(value, getter.getGenericReturnType()));
                        }
                    } catch (Exception _ex) {
                        getLogger().debug("Failed to read bound property {} for managed objects", pe.getKey().getName(), _ex);
                    }
                }
            }

            // objects implementing the Properties interface directly
            if (obj instanceof Properties p) {
                try {
                    Map<String, Variant<?>> all = p.GetAll(ifaceName);
                    if (all != null) {
                        props.putAll(all);
                    }
                } catch (RuntimeException _ex) {
                    getLogger().debug("GetAll failed for interface {} while building managed objects", ifaceName, _ex);
                }
            }

            byInterface.put(ifaceName, props);
        }
        return byInterface;
    }

    /**
     * Returns the object path of the closest exported {@link ObjectManager} which is an ancestor of the
     * given path, or {@code null} if none exists.
     *
     * @param _path object path to find a managing ObjectManager for
     *
     * @return ObjectManager object path or {@code null}
     */
    protected String findObjectManagerAncestor(String _path) {
        return doWithExportedObjectsAndReturn(RuntimeException.class, eos -> {
            String best = null;
            for (Entry<String, ExportedObject> e : eos.entrySet()) {
                String mgrPath = e.getKey();
                if (mgrPath == null || !(e.getValue().getObject().get() instanceof ObjectManager)) {
                    continue;
                }
                boolean ancestor = "/".equals(mgrPath) ? !"/".equals(_path) : _path.startsWith(mgrPath + "/");
                if (ancestor && (best == null || mgrPath.length() > best.length())) {
                    best = mgrPath;
                }
            }
            return best;
        });
    }

    /**
     * Emits an {@code InterfacesAdded} signal for the given newly exported object, if automatic
     * ObjectManager handling is enabled and the object lives below an exported ObjectManager.
     *
     * @param _objectPath path of the exported object
     */
    protected void emitInterfacesAdded(String _objectPath) {
        if (getConnectionConfig().isManualObjectManager()) {
            return;
        }
        String mgrPath = findObjectManagerAncestor(_objectPath);
        if (mgrPath == null) {
            return;
        }
        ExportedObject eo = doWithExportedObjectsAndReturn(RuntimeException.class, eos -> eos.get(_objectPath));
        if (eo == null) {
            return;
        }
        try {
            sendMessage(new ObjectManager.InterfacesAdded(mgrPath, new DBusPath(_objectPath), collectManagedInterfaces(eo)));
        } catch (DBusException _ex) {
            getLogger().warn("Failed to emit InterfacesAdded for {}", _objectPath, _ex);
        }
    }

    /**
     * Emits an {@code InterfacesRemoved} signal for an unexported object, if automatic ObjectManager
     * handling is enabled and the object lived below an exported ObjectManager.
     *
     * @param _objectPath path of the (now unexported) object
     * @param _interfaceNames the interface names the object provided
     */
    protected void emitInterfacesRemoved(String _objectPath, List<String> _interfaceNames) {
        if (getConnectionConfig().isManualObjectManager() || _interfaceNames.isEmpty()) {
            return;
        }
        String mgrPath = findObjectManagerAncestor(_objectPath);
        if (mgrPath == null) {
            return;
        }
        try {
            sendMessage(new ObjectManager.InterfacesRemoved(mgrPath, new DBusPath(_objectPath), _interfaceNames));
        } catch (DBusException _ex) {
            getLogger().warn("Failed to emit InterfacesRemoved for {}", _objectPath, _ex);
        }
    }

    /**
     * Collects the non-standard DBus interface names an exported object provides.
     *
     * @param _eo exported object
     *
     * @return list of interface names
     */
    protected List<String> collectInterfaceNames(ExportedObject _eo) {
        List<String> names = new ArrayList<>();
        for (Class<?> iface : _eo.getImplementedInterfaces()) {
            if (!DBusObjects.isStandardInterface(iface)) {
                names.add(DBusNamingUtil.getInterfaceName(iface));
            }
        }
        return names;
    }

}
