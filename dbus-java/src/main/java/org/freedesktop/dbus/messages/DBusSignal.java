/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.InternalSignal;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBusSignal extends Message {
    private static final Map<String, Class<? extends DBusSignal>>                            CLASS_CACHE       =
            new ConcurrentHashMap<>();

    private static final Map<Class<? extends DBusSignal>, Type[]>                            TYPE_CACHE        =
            new ConcurrentHashMap<>();

    private static final Map<Class<? extends DBusSignal>, Constructor<? extends DBusSignal>> CONSTRUCTOR_CACHE =
            new ConcurrentHashMap<>();

    private static final Map<String, String>                                                 SIGNAL_NAMES      =
            new ConcurrentHashMap<>();
    private static final Map<String, String>                                                 INT_NAMES         =
            new ConcurrentHashMap<>();

    private final Logger                                                                     logger            =
            LoggerFactory.getLogger(getClass());

    private Class<? extends DBusSignal>                                                      clazz;
    private boolean                                                                          bodydone          = false;
    private byte[]                                                                           blen;

    DBusSignal() {
    }

    public DBusSignal(String source, String path, String iface, String member, String sig, Object... args)
            throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.SIGNAL, (byte) 0);

        if (null == path || null == member || null == iface) {
            throw new MessageFormatException("Must specify object path, interface and signal name to Signals.");
        }
        getHeaders().put(Message.HeaderField.PATH, path);
        getHeaders().put(Message.HeaderField.MEMBER, member);
        getHeaders().put(Message.HeaderField.INTERFACE, iface);

        List<Object> hargs = new ArrayList<>();
        hargs.add(new Object[] {
                Message.HeaderField.PATH, new Object[] {
                        ArgumentType.OBJECT_PATH_STRING, path
                }
        });
        hargs.add(new Object[] {
                Message.HeaderField.INTERFACE, new Object[] {
                        ArgumentType.STRING_STRING, iface
                }
        });
        hargs.add(new Object[] {
                Message.HeaderField.MEMBER, new Object[] {
                        ArgumentType.STRING_STRING, member
                }
        });

        if (null != source) {
            getHeaders().put(Message.HeaderField.SENDER, source);
            hargs.add(new Object[] {
                    Message.HeaderField.SENDER, new Object[] {
                            ArgumentType.STRING_STRING, source
                    }
            });
        }

        if (null != sig) {
            hargs.add(new Object[] {
                    Message.HeaderField.SIGNATURE, new Object[] {
                            ArgumentType.SIGNATURE_STRING, sig
                    }
            });
            getHeaders().put(Message.HeaderField.SIGNATURE, sig);
            setArgs(args);
        }

        blen = new byte[4];
        appendBytes(blen);
        long newSerial = getSerial() + 1;
        setSerial(newSerial);
        append("ua(yv)", newSerial, hargs.toArray());
        pad((byte) 8);

        long counter = getByteCounter();
        if (null != sig) {
            append(sig, args);
        }
        marshallint(getByteCounter() - counter, blen, 0, 4);
        bodydone = true;
    }

    static void addInterfaceMap(String java, String dbus) {
        INT_NAMES.put(dbus, java);
    }

    static void addSignalMap(String java, String dbus) {
        SIGNAL_NAMES.put(dbus, java);
    }

    static DBusSignal createSignal(Class<? extends DBusSignal> c, String source, String objectpath, String sig,
            long serial, Object... parameters) throws DBusException {
        String type = "";
        if (null != c.getEnclosingClass()) {
            if (null != c.getEnclosingClass().getAnnotation(DBusInterfaceName.class)) {
                type = c.getEnclosingClass().getAnnotation(DBusInterfaceName.class).value();
            } else {
                type = AbstractConnection.DOLLAR_PATTERN.matcher(c.getEnclosingClass().getName()).replaceAll(".");
            }

        } else {
            throw new DBusException(
                    "Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
        }
        DBusSignal s = new InternalSignal(source, objectpath, type, c.getSimpleName(), sig, serial, parameters);
        s.clazz = c;
        return s;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DBusSignal> createSignalClass(String intname, String signame) throws DBusException {
        String name = intname + '$' + signame;
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
            } catch (ClassNotFoundException exCnf) {
            }
            name = name.replaceAll("\\.([^\\.]*)$", "\\$$1");
        } while (null == c && name.matches(".*\\..*"));
        if (null == c) {
            throw new DBusException("Could not create class from signal " + intname + '.' + signame);
        }
        CLASS_CACHE.put(name, c);
        return c;
    }

    @SuppressWarnings("unchecked")
    public DBusSignal createReal(AbstractConnection conn) throws DBusException {
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
        Type[] types = TYPE_CACHE.get(clazz);
        Constructor<? extends DBusSignal> con = CONSTRUCTOR_CACHE.get(clazz);
        if (null == types) {
            con = (Constructor<? extends DBusSignal>) clazz.getDeclaredConstructors()[0];
            CONSTRUCTOR_CACHE.put(clazz, con);
            Type[] ts = con.getGenericParameterTypes();
            types = new Type[ts.length - 1];
            for (int i = 1; i < ts.length; i++) {
                if (ts[i] instanceof TypeVariable) {
                    for (Type b : ((TypeVariable<GenericDeclaration>) ts[i]).getBounds()) {
                        types[i - 1] = b;
                    }
                } else {
                    types[i - 1] = ts[i];
                }
            }
            TYPE_CACHE.put(clazz, types);
        }

        try {
            DBusSignal s;
            Object[] args = Marshalling.deSerializeParameters(getParameters(), types, conn);
            if (null == args) {
                s = con.newInstance(getPath());
            } else {
                Object[] params = new Object[args.length + 1];
                params[0] = getPath();
                System.arraycopy(args, 0, params, 1, args.length);

                logger.debug("Creating signal of type {} with parameters {}", clazz, Arrays.deepToString(params));
                s = con.newInstance(params);
            }
            s.getHeaders().putAll(getHeaders());
            s.setWiredata(getWireData());
            s.setByteCounter(getWireData().length);
            return s;
        } catch (Exception _ex) {
            throw new DBusException(_ex);
        }
    }

    /**
     * Create a new signal. This contructor MUST be called by all sub classes.
     *
     * @param objectpath The path to the object this is emitted from.
     * @param args The parameters of the signal.
     * @throws DBusException This is thrown if the subclass is incorrectly defined.
     */
    @SuppressWarnings("unchecked")
    protected DBusSignal(String objectpath, Object... args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.SIGNAL, (byte) 0);

        if (!objectpath.matches(AbstractConnection.OBJECT_REGEX)) {
            throw new DBusException("Invalid object path: " + objectpath);
        }

        Class<? extends DBusSignal> tc = getClass();
        String member;
        if (tc.isAnnotationPresent(DBusMemberName.class)) {
            member = tc.getAnnotation(DBusMemberName.class).value();
        } else {
            member = tc.getSimpleName();
        }
        String iface = null;
        Class<? extends Object> enc = tc.getEnclosingClass();
        if (null == enc || !DBusInterface.class.isAssignableFrom(enc) || enc.getName().equals(enc.getSimpleName())) {
            throw new DBusException(
                    "Signals must be declared as a member of a class implementing DBusInterface which is the member of a package.");
        } else if (null != enc.getAnnotation(DBusInterfaceName.class)) {
            iface = enc.getAnnotation(DBusInterfaceName.class).value();
        } else {
            iface = AbstractConnection.DOLLAR_PATTERN.matcher(enc.getName()).replaceAll(".");
        }

        getHeaders().put(Message.HeaderField.PATH, objectpath);
        getHeaders().put(Message.HeaderField.MEMBER, member);
        getHeaders().put(Message.HeaderField.INTERFACE, iface);

        List<Object> hargs = new ArrayList<>();
        hargs.add(new Object[] {
                Message.HeaderField.PATH, new Object[] {
                        ArgumentType.OBJECT_PATH_STRING, objectpath
                }
        });
        hargs.add(new Object[] {
                Message.HeaderField.INTERFACE, new Object[] {
                        ArgumentType.STRING_STRING, iface
                }
        });
        hargs.add(new Object[] {
                Message.HeaderField.MEMBER, new Object[] {
                        ArgumentType.STRING_STRING, member
                }
        });

        String sig = null;
        if (0 < args.length) {
            try {
                Type[] types = TYPE_CACHE.get(tc);
                if (null == types) {
                    Constructor<? extends DBusSignal> con =
                            (Constructor<? extends DBusSignal>) tc.getDeclaredConstructors()[0];
                    CONSTRUCTOR_CACHE.put(tc, con);
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
                hargs.add(new Object[] {
                        Message.HeaderField.SIGNATURE, new Object[] {
                                ArgumentType.SIGNATURE_STRING, sig
                        }
                });
                getHeaders().put(Message.HeaderField.SIGNATURE, sig);
                setArgs(args);
            } catch (Exception e) {
                logger.debug("", e);
                throw new DBusException("Failed to add signal parameters: " + e.getMessage());
            }
        }

        blen = new byte[4];
        appendBytes(blen);
        long newSerial = getSerial() + 1;
        setSerial(newSerial);
        append("ua(yv)", newSerial, hargs.toArray());
        pad((byte) 8);
    }

    public void appendbody(AbstractConnection conn) throws DBusException {
        if (bodydone) {
            return;
        }

        Type[] types = TYPE_CACHE.get(getClass());
        Object[] args = Marshalling.convertParameters(getParameters(), types, conn);
        setArgs(args);
        String sig = getSig();

        long counter = getByteCounter();
        if (null != args && 0 < args.length) {
            append(sig, args);
        }
        marshallint(getByteCounter() - counter, blen, 0, 4);
        bodydone = true;
    }

    @Override
    public String toString() {
        return "DBusSignal [clazz=" + clazz + "]";
    }

}
