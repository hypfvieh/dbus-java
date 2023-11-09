package org.freedesktop.dbus.connections.base;

import org.freedesktop.dbus.*;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperties;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.errors.InvalidMethodArgument;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.errors.UnknownObject;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.CallbackHandler;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.*;
import org.freedesktop.dbus.messages.Error;
import org.freedesktop.dbus.messages.constants.Flags;
import org.freedesktop.dbus.propertyref.PropertyRef;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class containing most methods to handle/react to a message received on a connection. <br>
 * Part of the {@link AbstractConnectionBase} &rarr;  {@link ConnectionMethodInvocation}
 * &rarr; {@link ConnectionMessageHandler} &rarr; {@link AbstractConnection} hierarchy.
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-23
 */
public abstract sealed class ConnectionMessageHandler extends ConnectionMethodInvocation permits AbstractConnection {
    protected ConnectionMessageHandler(TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_transportConfig, _rsCfg);
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
            if (e.getKey().matches(_signal, false)) {
                handlers.addAll(e.getValue());
            }
        }

        for (Entry<DBusMatchRule, Queue<DBusSigHandler<DBusSignal>>> e : getGenericHandledSignals().entrySet()) {
            if (e.getKey().matches(_signal, false)) {
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
                        return;
                    }
                    ((DBusSigHandler<DBusSignal>) h).handle(rs);
                } catch (DBusException _ex) {
                    getLogger().warn("Exception while running signal handler '{}' for signal '{}':", h, _signal, _ex);
                    handleException(_signal, new DBusExecutionException("Error handling signal " + _signal.getInterface()
                            + "." + _signal.getName() + ": " + _ex.getMessage()));
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

                            fcbh.handleError(_err.getException());
                            getInfoMap().remove(Thread.currentThread());

                        } catch (Exception _ex) {
                            getLogger().debug("Exception while running error callback.", _ex);
                        }
                    }
                };
                getReceivingService().execErrorHandler(command);
            }

        } else {
            getPendingErrorQueue().add(_err);
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
                            Object convertRV = RemoteInvocationHandler.convertRV(_mr.getParameters(), fasr.getMethod(),
                                    fasr.getConnection());
                            fcbh.handle(convertRV);
                            getInfoMap().remove(Thread.currentThread());

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

    private void handleMessage(final MethodCall _methodCall) throws DBusException {
        getLogger().debug("Handling incoming method call: {}", _methodCall);

        ExportedObject exportObject;
        Method meth = null;
        Object o = null;

        if (null == _methodCall.getInterface() || _methodCall.getInterface().equals("org.freedesktop.DBus.Peer")
                || _methodCall.getInterface().equals("org.freedesktop.DBus.Introspectable")) {
            exportObject = getExportedObjects().get(null);
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

            exportObject = getExportedObjects().get(_methodCall.getPath());
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

            Object[] params = _methodCall.getParameters();
            if (handleDBusBoundProperties(exportObject, _methodCall, params)) {
                return;
            }

            if (meth == null) {
                meth = exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
                if (null == meth) {
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

    /**
     * Method which handles the magic related to {@link DBusBoundProperty} annotation.<br>
     * It takes care of proper method calling (calling Get/Set stuff on DBus Properties interface)<br>
     * and will also take care of converting wrapped Variant types.
     *
     * @param _exportObject exported object
     * @param _methodCall method to call
     * @param _params parameter to pass to method
     *
     * @return true if call was handled, false otherwise
     *
     * @throws DBusException when something fails
     */
    @SuppressWarnings("unchecked")
    private boolean handleDBusBoundProperties(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) throws DBusException {
        if (_params.length == 2 && _params[0] instanceof String
            && _params[1] instanceof String
            && _methodCall.getName().equals("Get")) {

            // 'Get' This MIGHT be a property reference
            PropertyRef propertyRef = new PropertyRef((String) _params[1], null, DBusProperty.Access.READ);
            Method propMeth = _exportObject.getPropertyMethods().get(propertyRef);
            if (propMeth != null) {
                // This IS a property reference
                Object object = _exportObject.getObject().get();

                getReceivingService().execMethodCallHandler(() -> {
                    _methodCall.setArgs(new Object[0]);
                    invokeMethodAndReply(_methodCall, propMeth, object, 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED));
                });

                return true;
            }
        } else if (_params.length == 3
            && _params[0] instanceof String
            && _params[1] instanceof String
            && _methodCall.getName().equals("Set")) {
            // 'Set' This MIGHT be a property reference

            PropertyRef propertyRef = new PropertyRef((String) _params[1], null, Access.WRITE);
            Method propMeth = _exportObject.getPropertyMethods().get(propertyRef);
            if (propMeth != null) {
                // This IS a property reference
                Object object = _exportObject.getObject().get();
                Class<?> type = PropertyRef.typeForMethod(propMeth);
                AtomicBoolean isVariant = new AtomicBoolean(false);

                Object val = Optional.ofNullable(_params[2])
                    .map(v -> {
                        if (v instanceof Variant<?> va) {
                            isVariant.set(true);
                            return va.getValue();
                        }
                        return v;
                    }).orElse(null);

                getReceivingService().execMethodCallHandler(() -> {
                    try {
                        Object myVal = val;
                        Parameter[] parameters = propMeth.getParameters();
                        // the setter method can only be used if it has just 1 parameter
                        if (parameters.length != 1) {
                            throw new InvalidMethodArgument("Expected method with one argument, but found " + parameters.length);
                        }
                        // take care of arrays:
                        // DBus only knows arrays of types, not lists or other collections.
                        // if the method which should be called wants a Collection we have to
                        // convert the array to a proper type
                        if (Collection.class.isAssignableFrom(parameters[0].getType())
                            && isVariant.get() && myVal != null && myVal.getClass().isArray()) {

                            if (Set.class.isAssignableFrom(parameters[0].getType())) {
                                myVal = new LinkedHashSet<>(Arrays.asList(Util.toObjectArray(myVal)));
                            } else { // assume list is fine for all other collection types
                                myVal = new ArrayList<>(Arrays.asList(Util.toObjectArray(myVal)));
                            }
                        }
                        _methodCall.setArgs(Marshalling.deSerializeParameters(new Object[] {myVal}, new Type[] {type}, this));
                        invokeMethodAndReply(_methodCall, propMeth, object, 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED));
                    } catch (Exception _ex) {
                        getLogger().debug("Failed to invoke method call on Properties", _ex);
                        handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                        return;
                    }
                });

                return true;
            }
        } else if (_params.length == 1 && _params[0] instanceof String
            && _methodCall.getName().equals("GetAll")) {
            // 'GetAll'
            Set<Entry<PropertyRef, Method>> allPropertyMethods = _exportObject.getPropertyMethods().entrySet();
            /* If there are no property methods on this object, just process as normal */
            if (!allPropertyMethods.isEmpty()) {
                Object object = _exportObject.getObject().get();
                Method meth = null;
                if (meth == null && object instanceof DBusProperties) {
                    meth = _exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
                    if (null == meth) {
                        sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                            "The method `%s.%s' does not exist on this object.", _methodCall.getInterface(), _methodCall.getName()))));
                        return true;
                    }
                } else if (meth == null) {
                    try {
                        meth = Properties.class.getDeclaredMethod("GetAll", String.class);
                    } catch (NoSuchMethodException | SecurityException _ex) {
                        getLogger().debug("Properties GetAll failed", _ex);
                        handleException(_methodCall,
                            new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                            _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage())));
                    }
                }

                Method originalMeth = meth;

                getReceivingService().execMethodCallHandler(() -> {
                    Map<String, Object> resultMap = new HashMap<>();
                    for (Entry<PropertyRef, Method> propEn : allPropertyMethods) {
                        Method propMeth = propEn.getValue();
                        if (propEn.getKey().getAccess() == Access.READ) {
                            try {
                                _methodCall.setArgs(new Object[0]);
                                Object val =  invokeMethod(_methodCall, propMeth, object);
                                resultMap.put(propEn.getKey().getName(), val);
                            } catch (Throwable _ex) {
                                getLogger().debug("", _ex);
                                handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                                return;
                            }
                        }
                    }

                    if (object instanceof DBusProperties) {
                        resultMap.putAll((Map<? extends String, ? extends Variant<?>>) setupAndInvoke(_methodCall, originalMeth, object, true));
                    }

                    try {
                        invokedMethodReply(_methodCall, originalMeth, resultMap);
                    } catch (DBusExecutionException _ex) {
                        getLogger().debug("Error invoking method call", _ex);
                        handleException(_methodCall, _ex);
                    } catch (Throwable _ex) {
                        getLogger().debug("Failed to invoke method call", _ex);
                        handleException(_methodCall,
                            new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                            _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage())));
                    }
                });
                return true;
            }
        }
        return false;
    }
}
