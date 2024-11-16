package org.freedesktop.dbus.messages;

import org.freedesktop.dbus.*;
import org.freedesktop.dbus.connections.base.AbstractConnectionBase;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.constants.ArgumentType;
import org.freedesktop.dbus.messages.constants.HeaderField;
import org.freedesktop.dbus.messages.constants.MessageType;
import org.freedesktop.dbus.utils.CommonRegexPattern;
import org.freedesktop.dbus.utils.DBusNamingUtil;
import org.freedesktop.dbus.utils.DBusObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DBusSignal extends Message {
    private static final Logger                                                    LOGGER              =
            LoggerFactory.getLogger(DBusSignal.class);

    private static final Map<String, Class<? extends DBusSignal>>                  CLASS_CACHE         =
            new ConcurrentHashMap<>();

    private static final Map<Class<? extends DBusSignal>, Type[]>                  TYPE_CACHE          =
            new ConcurrentHashMap<>();

    private static final Map<String, String>                                       SIGNAL_NAMES        =
            new ConcurrentHashMap<>();
    private static final Map<String, String>                                       INT_NAMES           =
            new ConcurrentHashMap<>();

    private static final Map<Class<? extends DBusSignal>, List<CachedConstructor>> CACHED_CONSTRUCTORS =
            new ConcurrentHashMap<>();

    private Class<? extends DBusSignal>                                            clazz;
    private boolean                                                                bodydone            = false;
    private byte[]                                                                 blen;

    DBusSignal() {
    }

    protected DBusSignal(byte _endianess, String _source, String _path, String _iface, String _member, String _sig, Object... _args)
            throws DBusException {
        super(_endianess, MessageType.SIGNAL, (byte) 0);

        if (null == _path || null == _member || null == _iface) {
            throw new MessageFormatException("Must specify object path, interface and signal name to Signals.");
        }

        List<Object> hargs = new ArrayList<>();
        hargs.add(createHeaderArgs(HeaderField.PATH, ArgumentType.OBJECT_PATH_STRING, _path));
        hargs.add(createHeaderArgs(HeaderField.INTERFACE, ArgumentType.STRING_STRING, _iface));
        hargs.add(createHeaderArgs(HeaderField.MEMBER, ArgumentType.STRING_STRING, _member));

        if (null != _source) {
            hargs.add(createHeaderArgs(HeaderField.SENDER, ArgumentType.STRING_STRING, _source));
        }

        if (null != _sig) {
            hargs.add(createHeaderArgs(HeaderField.SIGNATURE, ArgumentType.SIGNATURE_STRING, _sig));
            setArgs(_args);
        }

        padAndMarshall(hargs, getSerial(), _sig, _args);
        bodydone = true;
    }

    /**
     * Create a new signal. This constructor MUST be called by all sub classes.
     *
     * @param _objectPath The path to the object this is emitted from.
     * @param _args The parameters of the signal.
     * @throws DBusException This is thrown if the subclass is incorrectly defined.
     */
    protected DBusSignal(String _objectPath, Object... _args) throws DBusException {
        this((byte) 0, _objectPath, _args);
    }

    /**
     * Create a new signal. This contructor MUST be called by all sub classes.
     *
     * @param _endianess message endianess
     * @param _objectPath The path to the object this is emitted from.
     * @param _args The parameters of the signal.
     * @throws DBusException This is thrown if the subclass is incorrectly defined.
     */
    @SuppressWarnings("unchecked")
    protected DBusSignal(byte _endianess, String _objectPath, Object... _args) throws DBusException {
        super(_endianess, MessageType.SIGNAL, (byte) 0);

        DBusObjects.requireObjectPath(_objectPath);

        Class<? extends DBusSignal> tc = getClass();
        String member = DBusNamingUtil.getSignalName(tc);
        Class<? extends Object> enc = tc.getEnclosingClass();
        if (null == enc || !DBusInterface.class.isAssignableFrom(enc) || enc.getName().equals(enc.getSimpleName())) {
            throw new DBusException(
                    "Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
        }
        String iface = DBusNamingUtil.getInterfaceName(enc);

        List<Object> hargs = new ArrayList<>();
        hargs.add(createHeaderArgs(HeaderField.PATH, ArgumentType.OBJECT_PATH_STRING, _objectPath));
        hargs.add(createHeaderArgs(HeaderField.INTERFACE, ArgumentType.STRING_STRING, iface));
        hargs.add(createHeaderArgs(HeaderField.MEMBER, ArgumentType.STRING_STRING, member));

        String sig = null;
        if (0 < _args.length) {
            try {
                Type[] types = TYPE_CACHE.get(tc);
                if (null == types) {
                    Constructor<? extends DBusSignal> con =
                            (Constructor<? extends DBusSignal>) tc.getDeclaredConstructors()[0];
                    Type[] ts = con.getGenericParameterTypes();
                    types = new Type[ts.length - 1];
                    for (int i = 1; i <= types.length; i++) {
                        if (ts[i] instanceof TypeVariable) {
                            types[i - 1] = ((TypeVariable<GenericDeclaration>) ts[i]).getBounds()[0];
                        } else {
                            types[i - 1] = ts[i];
                        }
                    }
                    TYPE_CACHE.put(tc, types);
                }
                sig = Marshalling.getDBusType(types);
                hargs.add(createHeaderArgs(HeaderField.SIGNATURE, ArgumentType.SIGNATURE_STRING, sig));
                setArgs(_args);
            } catch (Exception _ex) {
                logger.debug("Error adding signal parameters", _ex);
                throw new DBusException("Failed to add signal parameters: " + _ex.getMessage());
            }
        }

        blen = new byte[4];
        appendBytes(blen);
        append("ua(yv)", getSerial(), hargs.toArray());
        pad((byte) 8);
    }

    static void addInterfaceMap(String _java, String _dbus) {
        INT_NAMES.put(_dbus, _java);
    }

    static void addSignalMap(String _java, String _dbus) {
        SIGNAL_NAMES.put(_dbus, _java);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DBusSignal> createSignalClass(String _intName, String _sigName) throws DBusException {
        String name = _intName + '$' + _sigName;
        Class<? extends DBusSignal> c = CLASS_CACHE.get(name);
        if (null == c) {
            c = DBusMatchRule.getCachedSignalType(name);
        }
        if (null != c) {
            return c;
        }
        do {
            try {
                c = (Class<? extends DBusSignal>) Class.forName(name);
            } catch (ClassNotFoundException _exCnf) {
                LOGGER.trace("Class not found for {}", name, _exCnf);
            }
            name = CommonRegexPattern.EXCEPTION_EXTRACT_PATTERN.matcher(name).replaceAll("\\$$1");
        } while (null == c && CommonRegexPattern.EXCEPTION_PARTIAL_PATTERN.matcher(name).matches());
        if (null == c) {
            throw new DBusException("Could not create class from signal " + _intName + '.' + _sigName);
        }
        CLASS_CACHE.put(name, c);
        return c;
    }

    /**
     * Called to create signal objects for object handlers.
     *
     * @param _conn connection on which the signal was received
     * @return DBusSignal or null
     *
     * @throws DBusException when reading signal fails
     */
    public DBusSignal createReal(AbstractConnectionBase _conn) throws DBusException {
        String intname = INT_NAMES.get(getInterface());
        String signame = SIGNAL_NAMES.get(getName());
        if (null == intname) {
            intname = getInterface();
        }
        if (null == signame) {
            signame = getName();
        }
        if (null == clazz) {
            clazz = createSignalClass(intname, signame);
        }

        logger.debug("Converting signal to type: {}", clazz);

        if (!CACHED_CONSTRUCTORS.containsKey(clazz)) {
            cacheConstructors(clazz);
        }

        List<CachedConstructor> list = CACHED_CONSTRUCTORS.get(clazz);

        Constructor<? extends DBusSignal> con = null;
        Type[] types = null;

        List<Type[]> constructorArgs = list.stream().map(c -> c.types).toList();

        Object[] parameters = getParameters(constructorArgs);

        // Get all classes required in constructor in order
        // Primitives will always be wrapped in their wrapper classes
        // because the parameters are received on the bus and will be converted
        // in 'Message.extractOne' method which will always return Object and not primitives
        List<Class<?>> wantedArgs = Arrays.stream(parameters)
                .map(Object::getClass)
                .collect(Collectors.toList());

        // find suitable constructor (by checking if parameter types are equal)
        for (CachedConstructor type : list) {
            if (type.matchesParameters(wantedArgs)) {
                con = type.constructor;
                types = type.types;
                break;
            }
        }
        if (con == null) {
            logger.warn("Could not find suitable constructor for class {} with argument-types: {}", clazz.getName(),
                    wantedArgs);
            return null;
        }

        try {
            DBusSignal s;
            Object[] args = Marshalling.deSerializeParameters(parameters, types, _conn);
            if (null == args) {
                s = con.newInstance(getPath());
            } else {
                Object[] params = new Object[args.length + 1];
                params[0] = getPath();
                System.arraycopy(args, 0, params, 1, args.length);
                s = con.newInstance(params);
            }

            // we have to setup endianess after construction otherwise all
            // signal implementations needs a new constructor :-\
            s.updateEndianess(_conn.getMessageFactory().getEndianess());
            s.setHeader(getHeader());
            s.setWireData(getWireData());
            s.setByteCounter(getWireData().length);
            return s;
        } catch (Exception _ex) {
            throw new DBusException(_ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void cacheConstructors(Class<? extends DBusSignal> _clazz) {
        List<CachedConstructor> list = new ArrayList<>();
        for (Constructor<?> constructor : _clazz.getDeclaredConstructors()) {
            Constructor<? extends DBusSignal> x = (Constructor<? extends DBusSignal>) constructor;
            list.add(new CachedConstructor(x));
        }

        CACHED_CONSTRUCTORS.put(_clazz, list);
    }

    public void appendbody(AbstractConnectionBase _conn) throws DBusException {
        if (bodydone) {
            return;
        }

        Type[] types = TYPE_CACHE.get(getClass());
        Object[] args = Marshalling.convertParameters(getParameters(), types, _conn);
        setArgs(args);
        String sig = getSig();

        long counter = getByteCounter();
        if (null != args && 0 < args.length) {
            append(sig, args);
        }
        marshallint(getByteCounter() - counter, blen, 0, 4);
        bodydone = true;
    }

    private static class CachedConstructor {
        private final Constructor<? extends DBusSignal> constructor;
        private final List<Class<?>>                    parameterTypes;
        private final Type[]                            types;

        CachedConstructor(Constructor<? extends DBusSignal> _constructor) {
            constructor = _constructor;
            parameterTypes = Arrays.stream(constructor.getParameterTypes())
                    .skip(1)
                    .map(c -> {
                        // convert primitives to wrapper classes so we can compare it to parameter classes later
                        if (c.isPrimitive()) {
                            return wrap(c);
                        }
                        return c;
                    })
                    .toList();
            types = createTypes(constructor);
        }

        public boolean matchesParameters(List<Class<?>> _wantedArgs) {
            if (parameterTypes == null || _wantedArgs == null) {
                return false;
            }

            if (parameterTypes.size() != _wantedArgs.size()) {
                return false;
            }

            for (int i = 0; i < parameterTypes.size(); i++) {
                Class<?> class1 = parameterTypes.get(i);

                if (Enum.class.isAssignableFrom(class1) && String.class.equals(_wantedArgs.get(i))) {
                    continue;
                } else if (DBusInterface.class.isAssignableFrom(class1) && ObjectPath.class.equals(_wantedArgs.get(i))) {
                    continue;
                } else if (DBusInterface.class.isAssignableFrom(class1) && DBusPath.class.equals(_wantedArgs.get(i))) {
                    continue;
                } else if (Struct.class.isAssignableFrom(class1) && Object[].class.equals(_wantedArgs.get(i))) {
                    continue;
                } else if (class1.isAssignableFrom(_wantedArgs.get(i))) {
                    continue;
                } else {
                    return false;
                }
            }

            return true;
        }

        @SuppressWarnings("unchecked")
        private static Type[] createTypes(Constructor<? extends DBusSignal> _constructor) {
            Type[] ts = _constructor.getGenericParameterTypes();
            Type[] types = new Type[ts.length - 1];
            for (int i = 1; i <= types.length; i++) {
                if (ts[i] instanceof TypeVariable) {
                    types[i - 1] = ((TypeVariable<GenericDeclaration>) ts[i]).getBounds()[0];
                } else {
                    types[i - 1] = ts[i];
                }
            }
            return types;
        }

        @SuppressWarnings("unchecked")
        private static <T> Class<T> wrap(Class<T> _clz) {
            return (Class<T>) MethodType.methodType(_clz).wrap().returnType();
        }
    }
}
