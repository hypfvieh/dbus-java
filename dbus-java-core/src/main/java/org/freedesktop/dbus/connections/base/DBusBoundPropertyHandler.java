package org.freedesktop.dbus.connections.base;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal;
import org.freedesktop.dbus.annotations.PropertiesEmitsChangedSignal.EmitChangeSignal;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.connections.impl.ConnectionConfig;
import org.freedesktop.dbus.errors.InvalidMethodArgument;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.constants.Flags;
import org.freedesktop.dbus.propertyref.PropRefRemoteHandler;
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
 * Abstract class containing methods for handling DBus properties and {@link DBusBoundProperty} annotation. <br>
 * Part of the {@link AbstractConnectionBase} &rarr;  {@link ConnectionMethodInvocation}
 * &rarr; {@link DBusBoundPropertyHandler} &rarr; {@link ConnectionMessageHandler} &rarr; {@link AbstractConnection} hierarchy.
 *
 * @author hypfvieh
 * @since 5.1.0 - 2024-03-18
 */
public abstract sealed class DBusBoundPropertyHandler extends ConnectionMethodInvocation permits ConnectionMessageHandler {

    private static final Method PROP_GETALL_METHOD = PropRefRemoteHandler.getPropertiesMethod("GetAll", String.class);

    protected DBusBoundPropertyHandler(ConnectionConfig _conCfg, TransportConfig _transportConfig, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_conCfg, _transportConfig, _rsCfg);
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
     * @return Any of:<br>
     *
     * {@link PropHandled#HANDLED} when property was defined by annotation and was handled by this method<br>
     * {@link PropHandled#NOT_HANDLED} when object implements DBus Properties but the requested property was not defined by annotation<br>
     * {@link PropHandled#NO_PROPERTY} when property is not defined by annotation and object does not implement DBus Properties<br>
     *
     * @throws DBusException when something fails
     */
    protected PropHandled handleDBusBoundProperties(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) throws DBusException {
        if (_params.length == 2 && _params[0] instanceof String
            && _params[1] instanceof String
            && _methodCall.getName().equals("Get")) {
            // 'Get'
            return handleGet(_exportObject, _methodCall, _params);

        } else if (_params.length == 3
            && _params[0] instanceof String
            && _params[1] instanceof String
            && _methodCall.getName().equals("Set")) {
            // 'Set'
            return handleSet(_exportObject, _methodCall, _params);

        } else if (_params.length == 1 && _params[0] instanceof String
            && _methodCall.getName().equals("GetAll")) {
            // 'GetAll'
            return handleGetAll(_exportObject, _methodCall);
        }
        return PropHandled.NOT_HANDLED;
    }

    /**
     * Called when 'GetAll' method of DBus {@link Properties} interface is called.
     *
     * @param _exportObject exported object
     * @param _methodCall method call
     *
     * @return {@link PropHandled#HANDLED} when call was handled {@link PropHandled#NOT_HANDLED} otherwise
     *
     * @throws DBusException when handling fails
     */
    @SuppressWarnings("unchecked")
    protected PropHandled handleGetAll(ExportedObject _exportObject, final MethodCall _methodCall) throws DBusException {
        Set<Entry<PropertyRef, Method>> allPropertyMethods = _exportObject.getPropertyMethods().entrySet();
        /* If there are no property methods on this object, just process as normal */
        if (!allPropertyMethods.isEmpty()) {
            Object object = _exportObject.getObject().get();
            Method meth = null;
            if (object instanceof Properties) {
                meth = _exportObject.getMethods().get(new MethodTuple(_methodCall.getName(), _methodCall.getSig()));
                if (null == meth) {
                    sendMessage(getMessageFactory().createError(_methodCall, new UnknownMethod(String.format(
                        "The method `%s.%s' does not exist on this object.", _methodCall.getInterface(), _methodCall.getName()))));
                    return PropHandled.HANDLED;
                }
            } else {
                meth = PROP_GETALL_METHOD;
            }

            Method originalMeth = meth;

            getReceivingService().execMethodCallHandler(() -> {
                Map<String, Object> resultMap = new HashMap<>();
                for (Entry<PropertyRef, Method> propEn : allPropertyMethods) {
                    Method propMeth = propEn.getValue();
                    if (propEn.getKey().getAccess() == Access.READ) {
                        try {
                            _methodCall.setArgs(new Object[0]);
                            Object val = invokeMethod(_methodCall, propMeth, object);

                            // when the value is a collection, array or map, wrap them in a proper variant type
                            if (val != null && val.getClass().isArray() || val instanceof Collection || val instanceof Map) {
                                String[] dataType = Marshalling.getDBusType(propEn.getValue().getGenericReturnType());
                                String dataTypeStr = String.join("", dataType);
                                getLogger().trace("Creating embedded Array/Collection/Map of type {}", dataTypeStr);
                                val = new Variant<>(val, dataTypeStr);
                            }

                            resultMap.put(propEn.getKey().getName(), val);
                        } catch (Throwable _ex) {
                            getLogger().debug("Error executing method {} on method call {}", propMeth, _methodCall, _ex);
                            handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                        }
                    }
                }

                // this object implements Properties, so we have to query for these properties as well as
                // collecting the properties only available by annotations
                if (object instanceof Properties) {
                    _methodCall.setArgs(new Object[] {_methodCall.getInterface()});
                    resultMap.putAll((Map<String, ? extends Variant<?>>) setupAndInvoke(_methodCall, originalMeth, object, true));
                }

                try {
                    invokedMethodReply(_methodCall, originalMeth, resultMap);
                } catch (DBusExecutionException _ex) {
                    getLogger().debug("Error invoking method call", _ex);
                    handleException(_methodCall, _ex);
                } catch (Throwable _ex) {
                    getLogger().debug("Failed to invoke method call", _ex);
                    handleException(_methodCall,
                        new DBusExecutionException("Error Executing Method %s.%s: %s".formatted(
                            _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage()), _ex));
                }
            });
            return PropHandled.HANDLED;
        }
        return PropHandled.NOT_HANDLED;
    }

    /**
     * Called when 'Get' method of DBus {@link Properties} interface is called.
     *
     * @param _exportObject exported object
     * @param _methodCall method call
     * @param _params parameters for method call
     *
     * @return Any of:<br>
     *
     * {@link PropHandled#HANDLED} when property was defined by annotation and was handled by this method<br>
     * {@link PropHandled#NOT_HANDLED} when object implements DBus Properties but the requested property was not defined by annotation<br>
     * {@link PropHandled#NO_PROPERTY} when property is not defined by annotation and object does not implement DBus Properties<br>
     */
    protected PropHandled handleGet(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) {
        PropertyRef propertyRef = new PropertyRef((String) _params[1], null, DBusProperty.Access.READ);
        Method propMeth = _exportObject.getPropertyMethods().get(propertyRef);
        if (propMeth != null) {
            // This IS a property reference
            Object object = _exportObject.getObject().get();

            getReceivingService().execMethodCallHandler(() -> {
                _methodCall.setArgs(new Object[0]);
                invokeMethodAndReply(_methodCall, propMeth, object, 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED));
            });

            return PropHandled.HANDLED;
        } else if (_exportObject.getImplementedInterfaces().contains(Properties.class)) {
            return PropHandled.NOT_HANDLED;
        } else {
            return PropHandled.NO_PROPERTY;
        }
    }

    /**
     * Called when 'Set' method of DBus {@link Properties} interface is called.
     *
     * @param _exportObject exported object
     * @param _methodCall method call
     * @param _params method call parameters
     *
     * @return {@link PropHandled#HANDLED} when property was definied by annotation, {@link PropHandled#NOT_HANDLED} otherwise
     */
    protected PropHandled handleSet(ExportedObject _exportObject, final MethodCall _methodCall, Object[] _params) {

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
                    _methodCall.setArgs(Marshalling.deSerializeParameters(new Object[] {myVal}, new Type[] {type}, this, true));
                    boolean noReply = 1 == (_methodCall.getFlags() & Flags.NO_REPLY_EXPECTED);
                    if (invokeSetterAndReply(_methodCall, propMeth, object, noReply)) {
                        // property was changed successfully; optionally announce it via PropertiesChanged
                        emitPropertiesChangedIfEnabled(_exportObject, _methodCall, propMeth,
                            (String) _params[0], (String) _params[1], myVal);
                    }
                } catch (Exception _ex) {
                    getLogger().debug("Failed to invoke method call on Properties", _ex);
                    handleException(_methodCall, new UnknownMethod("Failure in de-serializing message: " + _ex));
                }
            });
            return PropHandled.HANDLED;
        }
        return PropHandled.NOT_HANDLED;

    }

    /**
     * Invokes a property setter and sends the (void) method reply, mirroring the error handling of
     * {@link ConnectionMethodInvocation#invokeMethodAndReply(MethodCall, Method, Object, boolean)} but
     * reporting whether the setter completed successfully so the caller can decide whether to emit a
     * PropertiesChanged signal.
     *
     * @param _methodCall the Set method call
     * @param _setter the setter method
     * @param _object the exported object instance
     * @param _noReply whether a reply is expected
     *
     * @return {@code true} if the setter was invoked without error, {@code false} otherwise
     */
    private boolean invokeSetterAndReply(MethodCall _methodCall, Method _setter, Object _object, boolean _noReply) {
        try {
            invokeMethod(_methodCall, _setter, _object);
            if (!_noReply) {
                invokedMethodReply(_methodCall, _setter, null);
            }
            return true;
        } catch (DBusExecutionException _ex) {
            getLogger().debug("Failed to invoke property setter", _ex);
            handleException(_methodCall, _ex);
        } catch (Throwable _ex) {
            getLogger().debug("Error invoking property setter {}", _methodCall, _ex);
            handleException(_methodCall, new DBusExecutionException(String.format("Error Executing Method %s.%s: %s",
                _methodCall.getInterface(), _methodCall.getName(), _ex.getMessage()), _ex));
        }
        return false;
    }

    /**
     * Emits an {@code org.freedesktop.DBus.Properties.PropertiesChanged} signal for a changed bound
     * property, if enabled on the connection ({@link ConnectionConfig#isAutoEmitPropertiesChanged()})
     * and permitted by the property's {@link EmitChangeSignal} value.
     *
     * @param _exportObject the exported object
     * @param _methodCall the originating Set call (used for the object path)
     * @param _setter the property setter method (source of the EmitChangeSignal annotation)
     * @param _propertyInterface the interface name the property belongs to (from the Set arguments)
     * @param _propertyName the property name
     * @param _setValue the value that was set (fallback when no getter is available)
     */
    private void emitPropertiesChangedIfEnabled(ExportedObject _exportObject, MethodCall _methodCall, Method _setter,
            String _propertyInterface, String _propertyName, Object _setValue) {
        if (!getConnectionConfig().isAutoEmitPropertiesChanged()) {
            return;
        }
        EmitChangeSignal emit = resolveEmitChangeSignal(_setter);
        if (emit == EmitChangeSignal.CONST || emit == EmitChangeSignal.FALSE) {
            return;
        }

        try {
            Map<String, Variant<?>> changed = new HashMap<>();
            List<String> invalidated = new ArrayList<>();

            if (emit == EmitChangeSignal.INVALIDATES) {
                invalidated.add(_propertyName);
            } else { // TRUE - include the current value
                Method getter = _exportObject.getPropertyMethods().get(new PropertyRef(_propertyName, null, Access.READ));
                Object value;
                Type valueType;
                if (getter != null) {
                    value = getter.invoke(_exportObject.getObject().get());
                    valueType = getter.getGenericReturnType();
                } else { // write-only property - fall back to the value that was set
                    value = _setValue;
                    valueType = _setter.getGenericParameterTypes()[0];
                }
                if (value == null) {
                    getLogger().debug("Not emitting PropertiesChanged for {}.{}: value is null", _propertyInterface, _propertyName);
                    return;
                }
                changed.put(_propertyName, toVariant(value, valueType));
            }

            sendMessage(new Properties.PropertiesChanged(_methodCall.getPath(), _propertyInterface, changed, invalidated));
        } catch (Exception _ex) {
            getLogger().warn("Failed to emit PropertiesChanged for property {}.{}", _propertyInterface, _propertyName, _ex);
        }
    }

    /**
     * Resolves the effective {@link EmitChangeSignal} for a bound property. A non-default value on the
     * {@link DBusBoundProperty#emitChangeSignal()} method annotation takes precedence, followed by the
     * interface-global {@link PropertiesEmitsChangedSignal} annotation, defaulting to {@link EmitChangeSignal#TRUE}.
     *
     * @param _setter setter method of the property
     * @return effective EmitChangeSignal
     */
    private EmitChangeSignal resolveEmitChangeSignal(Method _setter) {
        DBusBoundProperty boundProperty = _setter.getAnnotation(DBusBoundProperty.class);
        if (boundProperty != null && boundProperty.emitChangeSignal() != EmitChangeSignal.TRUE) {
            return boundProperty.emitChangeSignal();
        }
        PropertiesEmitsChangedSignal global = _setter.getDeclaringClass().getAnnotation(PropertiesEmitsChangedSignal.class);
        if (global != null) {
            return global.value();
        }
        return EmitChangeSignal.TRUE;
    }

    /**
     * Wraps a property value in a {@link Variant}, computing the DBus signature for array/collection/map
     * values from the given type (as {@code GetAll} does).
     *
     * @param _value value to wrap (must not be {@code null})
     * @param _type generic type of the value (getter return type or setter parameter type)
     * @return the wrapped variant
     *
     * @throws DBusException when the DBus type cannot be determined
     */
    protected Variant<?> toVariant(Object _value, Type _type) throws DBusException {
        if (_value.getClass().isArray() || _value instanceof Collection || _value instanceof Map) {
            String signature = String.join("", Marshalling.getDBusType(_type));
            return new Variant<>(_value, signature);
        }
        return new Variant<>(_value);
    }

    enum PropHandled {
        /** Property request was handled. */
        HANDLED,
        /** Property request was not handled. */
        NOT_HANDLED,
        /** Property was not handled and Properties interface was not defined on exported object. */
        NO_PROPERTY
    }
}
