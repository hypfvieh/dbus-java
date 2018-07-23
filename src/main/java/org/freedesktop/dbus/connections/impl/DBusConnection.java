/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.connections.impl;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.SignalTuple;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.UInt32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.util.FileIoUtil;
import com.github.hypfvieh.util.StringUtil;
import com.github.hypfvieh.util.SystemUtil;

/**
 * Handles a connection to DBus.
 * <p>
 * This is a Singleton class, only 1 connection to the SYSTEM or SESSION busses can be made. Repeated calls to
 * getConnection will return the same reference.
 * </p>
 * <p>
 * Signal Handlers and method calls from remote objects are run in their own threads, you MUST handle the concurrency
 * issues.
 * </p>
 */
public final class DBusConnection extends AbstractConnection {
    private final Logger                             logger                     = LoggerFactory.getLogger(getClass());

    public static final String                       DEFAULT_SYSTEM_BUS_ADDRESS =
            "unix:path=/var/run/dbus/system_bus_socket";

    private List<String>                             busnames;

    private static final ConcurrentMap<String, DBusConnection> CONNECTIONS                = new ConcurrentHashMap<>();
    private DBus                                     dbus;

    private final String                             machineId;

    /** Count how many 'connections' we manage internally.
     * This is required because a {@link DBusConnection} to the same address will always return the same object and
     * the 'real' disconnection should only occur when there is no second/third/whatever connection is left. */
    private final AtomicInteger                      concurrentConnections              = new AtomicInteger(1);


    /**
     * Connect to the BUS. If a connection already exists to the specified Bus, a reference to it is returned. Will
     * always register our own session to Dbus.
     *
     * @param address
     *            The address of the bus to connect to
     * @throws DBusException
     *             If there is a problem connecting to the Bus.
     * @return {@link DBusConnection}
     */
    public static DBusConnection getConnection(String address) throws DBusException {
        return getConnection(address, true);
    }

    /**
     * Connect to the BUS. If a connection already exists to the specified Bus, a reference to it is returned. Will
     * register our own session to DBus if registerSelf is true (default).
     *
     * @param address
     *            The address of the bus to connect to
     * @param registerSelf
     *            register own session in dbus
     * @throws DBusException
     *             If there is a problem connecting to the Bus.
     * @return {@link DBusConnection}
     */
    public static DBusConnection getConnection(String address, boolean registerSelf) throws DBusException {

        //CONNECTIONS.getOrDefault(address, defaultValue)
        synchronized (CONNECTIONS) {
            DBusConnection c = CONNECTIONS.get(address);
            if (c != null) {
                c.concurrentConnections.incrementAndGet();
                return c;
            } else {
                c = new DBusConnection(address, registerSelf, getDbusMachineId());
                // do not increment connection counter here, it always starts at 1 on new objects!
                //c.getConcurrentConnections().incrementAndGet();
                CONNECTIONS.put(address, c);
                return c;
            }
        }
    }

    private static DBusConnection getConnection(Supplier<String> _addressGenerator, boolean _registerSelf) throws DBusException {
        if (_addressGenerator == null) {
            throw new DBusException("Invalid address generator");
        }
        String address = _addressGenerator.get();
        if (address == null) {
            throw new DBusException("null is not a valid DBUS address");
        }
        return getConnection(address, _registerSelf);
    }

    /**
     * Connect to the BUS. If a connection already exists to the specified Bus, a reference to it is returned.
     *
     * @param bustype
     *            The Bus to connect to.
     * @see #SYSTEM
     * @see #SESSION
     *
     * @return {@link DBusConnection}
     *
     * @throws DBusException
     *             If there is a problem connecting to the Bus.
     *
     */
    public static DBusConnection getConnection(DBusBusType bustype) throws DBusException {

        switch (bustype) {
            case SYSTEM:
                DBusConnection systemConnection = getConnection(() -> {
                    String bus = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
                    if (bus == null) {
                        bus = DEFAULT_SYSTEM_BUS_ADDRESS;
                    }
                    return bus;
                }, true);
                return systemConnection;
            case SESSION:
                DBusConnection sessionConnection = getConnection(() -> {
                    String s = null;

                    // MacOS support: e.g DBUS_LAUNCHD_SESSION_BUS_SOCKET=/private/tmp/com.apple.launchd.4ojrKe6laI/unix_domain_listener
                    if (SystemUtil.isMacOs()) {
                        s = "unix:path=" + System.getenv("DBUS_LAUNCHD_SESSION_BUS_SOCKET");;

                    } else { // all others (linux)
                        s = System.getenv("DBUS_SESSION_BUS_ADDRESS");
                    }

                    if (s == null) {
                        // address gets stashed in $HOME/.dbus/session-bus/`dbus-uuidgen --get`-`sed 's/:\(.\)\..*/\1/' <<<
                        // $DISPLAY`
                        String display = System.getenv("DISPLAY");
                        if (null == display) {
                            throw new RuntimeException("Cannot Resolve Session Bus Address");
                        }
                        if (!display.startsWith(":") && display.contains(":")) { // display seems to be a remote display
                                                                                 // (e.g. X forward through SSH)
                            display = display.substring(display.indexOf(':'));
                        }

                        try {
                            String uuid = getDbusMachineId();
                            String homedir = System.getProperty("user.home");
                            File addressfile = new File(homedir + "/.dbus/session-bus",
                                    uuid + "-" + display.replaceAll(":([0-9]*)\\..*", "$1"));
                            if (!addressfile.exists()) {
                                throw new RuntimeException("Cannot Resolve Session Bus Address");
                            }
                            Properties readProperties = FileIoUtil.readProperties(addressfile);
                            String sessionAddress = readProperties.getProperty("DBUS_SESSION_BUS_ADDRESS");
                            if (StringUtil.isEmpty(sessionAddress)) {
                                throw new RuntimeException("Cannot Resolve Session Bus Address");
                            }
                            return sessionAddress;
                        } catch (DBusException _ex) {
                            throw new RuntimeException("Cannot Resolve Session Bus Address", _ex);
                        }
                    }

                    return s;

                }, true);

                return sessionConnection;
            default:
                throw new DBusException("Invalid Bus Type: " + bustype);
        }

    }

    private AtomicInteger getConcurrentConnections() {
        return concurrentConnections;
    }

    /**
     * Extracts the machine-id usually found in /var/lib/dbus/machine-id.
     *
     * @return machine-id string, never null
     * @throws DBusException if machine-id could not be found
     */
    public static String getDbusMachineId() throws DBusException {
        File uuidfile = new File("/var/lib/dbus/machine-id");
        if (!uuidfile.exists()) {
            throw new DBusException("Cannot Resolve Session Bus Address");
        }

        String uuid = FileIoUtil.readFileToString(uuidfile);
        if (StringUtil.isEmpty(uuid)) {
            throw new DBusException("Cannot Resolve Session Bus Address: MachineId file is empty.");
        }

        return uuid;
    }

    private DBusConnection(String address, boolean registerSelf, String _machineId) throws DBusException {
        super(address);
        busnames = new ArrayList<>();
        machineId = _machineId;
        // start listening for calls
        listen();

        // register disconnect handlers
        DBusSigHandler<?> h = new SigHandler();
        addSigHandlerWithoutMatch(org.freedesktop.dbus.interfaces.Local.Disconnected.class, h);
        addSigHandlerWithoutMatch(org.freedesktop.DBus.NameAcquired.class, h);

        // register ourselves if not disabled
        if (registerSelf) {
            dbus = getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            try {
                busnames.add(dbus.Hello());
            } catch (DBusExecutionException dbee) {
                logger.debug("", dbee);
                throw new DBusException(dbee.getMessage());
            }
        }
    }

    protected DBusInterface dynamicProxy(String source, String path) throws DBusException {
        logger.debug("Introspecting {} on {} for dynamic proxy creation", path, source);
        try {
            Introspectable intro = getRemoteObject(source, path, Introspectable.class);
            String data = intro.Introspect();
            logger.trace("Got introspection data: {}", data);

            String[] tags = data.split("[<>]");
            List<String> ifaces = new ArrayList<>();
            for (String tag : tags) {
                if (tag.startsWith("interface")) {
                    ifaces.add(tag.replaceAll("^interface *name *= *['\"]([^'\"]*)['\"].*$", "$1"));
                }
            }
            List<Class<?>> ifcs = new ArrayList<>();
            for (String iface : ifaces) {
                // if this is a default DBus interface, look for it in our package structure
                if (iface.startsWith("org.freedesktop.DBus.")) {
                    iface = iface.replaceAll("^.*\\.([^\\.]+)$", DBusInterface.class.getPackage().getName() + ".$1");
                }

                logger.debug("Trying interface {}", iface);
                int j = 0;
                while (j >= 0) {
                    try {
                        Class<?> ifclass = Class.forName(iface);
                        if (!ifcs.contains(ifclass)) {
                            ifcs.add(ifclass);
                        }
                        break;
                    } catch (Exception e) {
                    }
                    j = iface.lastIndexOf(".");
                    char[] cs = iface.toCharArray();
                    if (j >= 0) {
                        cs[j] = '$';
                        iface = String.valueOf(cs);
                    }
                }
            }

            // interface could not be found, we guess that this exported object at least support DBusInterface
            if (ifcs.isEmpty()) {
                // throw new DBusException("Could not find an interface to cast to");
                ifcs.add(DBusInterface.class);
            }

            RemoteObject ro = new RemoteObject(source, path, null, false);
            DBusInterface newi = (DBusInterface) Proxy.newProxyInstance(ifcs.get(0).getClassLoader(),
                    ifcs.toArray(new Class[0]), new RemoteInvocationHandler(this, ro));
            getImportedObjects().put(newi, ro);
            return newi;
        } catch (Exception e) {
            logger.debug("", e);
            throw new DBusException(
                    String.format("Failed to create proxy object for %s exported by %s. Reason: %s", path,
                            source, e.getMessage()));
        }
    }

    @Override
    public DBusInterface getExportedObject(String source, String path) throws DBusException {
        ExportedObject o = null;
        synchronized (getExportedObjects()) {
            o = getExportedObjects().get(path);
        }
        if (null != o && null == o.getObject().get()) {
            unExportObject(path);
            o = null;
        }
        if (null != o) {
            return o.getObject().get();
        }
        if (null == source) {
            throw new DBusException("Not an object exported by this connection and no remote specified");
        }
        return dynamicProxy(source, path);
    }

    /**
     * Release a bus name. Releases the name so that other people can use it
     *
     * @param busname
     *            The name to release. MUST be in dot-notation like "org.freedesktop.local"
     * @throws DBusException
     *             If the busname is incorrectly formatted.
     */
    public void releaseBusName(String busname) throws DBusException {
        if (!busname.matches(BUSNAME_REGEX) || busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name");
        }
        synchronized (this.busnames) {
            try {
                dbus.ReleaseName(busname);
            } catch (DBusExecutionException dbee) {
                logger.debug("", dbee);
                throw new DBusException(dbee.getMessage());
            }
            this.busnames.remove(busname);
        }
    }

    /**
     * Request a bus name. Request the well known name that this should respond to on the Bus.
     *
     * @param busname
     *            The name to respond to. MUST be in dot-notation like "org.freedesktop.local"
     * @throws DBusException
     *             If the register name failed, or our name already exists on the bus. or if busname is incorrectly
     *             formatted.
     */
    public void requestBusName(String busname) throws DBusException {
        if (!busname.matches(BUSNAME_REGEX) || busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name");
        }
        synchronized (this.busnames) {
            UInt32 rv;
            try {
                rv = dbus.RequestName(busname,
                        new UInt32(DBus.DBUS_NAME_FLAG_REPLACE_EXISTING | DBus.DBUS_NAME_FLAG_DO_NOT_QUEUE));
            } catch (DBusExecutionException dbee) {
                logger.debug("", dbee);
                throw new DBusException(dbee.getMessage());
            }
            switch (rv.intValue()) {
            case DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER:
                break;
            case DBus.DBUS_REQUEST_NAME_REPLY_IN_QUEUE:
                throw new DBusException("Failed to register bus name");
            case DBus.DBUS_REQUEST_NAME_REPLY_EXISTS:
                throw new DBusException("Failed to register bus name");
            case DBus.DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER:
                break;
            default:
                break;
            }
            this.busnames.add(busname);
        }
    }

    /**
     * Returns the unique name of this connection.
     *
     * @return unique name
     */
    public String getUniqueName() {
        return busnames.get(0);
    }

    /**
     * Returns all the names owned by this connection.
     *
     * @return connection names
     */
    public String[] getNames() {
        Set<String> names = new TreeSet<String>();
        names.addAll(busnames);
        return names.toArray(new String[0]);
    }

    public <I extends DBusInterface> I getPeerRemoteObject(String busname, String objectpath, Class<I> type)
            throws DBusException {
        return getPeerRemoteObject(busname, objectpath, type, true);
    }

    /**
     * Return a reference to a remote object. This method will resolve the well known name (if given) to a unique bus
     * name when you call it. This means that if a well known name is released by one process and acquired by another
     * calls to objects gained from this method will continue to operate on the original process.
     *
     * This method will use bus introspection to determine the interfaces on a remote object and so <b>may block</b> and
     * <b>may fail</b>. The resulting proxy object will, however, be castable to any interface it implements. It will
     * also autostart the process if applicable. Also note that the resulting proxy may fail to execute the correct
     * method with overloaded methods and that complex types may fail in interesting ways. Basically, if something odd
     * happens, try specifying the interface explicitly.
     *
     * @param busname
     *            The bus name to connect to. Usually a well known bus name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param objectpath
     *            The path on which the process is exporting the object.$
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted.
     */
    public DBusInterface getPeerRemoteObject(String busname, String objectpath) throws DBusException {
        if (null == busname) {
            throw new DBusException("Invalid bus name: null");
        }

        if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX)) || busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + busname);
        }

        String unique = dbus.GetNameOwner(busname);

        return dynamicProxy(unique, objectpath);
    }

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * This method will use bus introspection to determine the interfaces on a remote object and so <b>may block</b> and
     * <b>may fail</b>. The resulting proxy object will, however, be castable to any interface it implements. It will
     * also autostart the process if applicable. Also note that the resulting proxy may fail to execute the correct
     * method with overloaded methods and that complex types may fail in interesting ways. Basically, if something odd
     * happens, try specifying the interface explicitly.
     *
     * @param busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param objectpath
     *            The path on which the process is exporting the object.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted.
     */
    public DBusInterface getRemoteObject(String busname, String objectpath) throws DBusException {
        if (null == busname) {
            throw new DBusException("Invalid bus name: null");
        }
        if (null == objectpath) {
            throw new DBusException("Invalid object path: null");
        }

        if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX)) || busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + busname);
        }

        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }

        return dynamicProxy(busname, objectpath);
    }

    /**
     * Return a reference to a remote object. This method will resolve the well known name (if given) to a unique bus
     * name when you call it. This means that if a well known name is released by one process and acquired by another
     * calls to objects gained from this method will continue to operate on the original process.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param busname
     *            The bus name to connect to. Usually a well known bus name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param objectpath
     *            The path on which the process is exporting the object.$
     * @param type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @param autostart
     *            Disable/Enable auto-starting of services in response to calls on this object. Default is enabled; when
     *            calling a method with auto-start enabled, if the destination is a well-known name and is not owned the
     *            bus will attempt to start a process to take the name. When disabled an error is returned immediately.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted or type is not in a package.
     */
    public <I extends DBusInterface> I getPeerRemoteObject(String busname, String objectpath, Class<I> type,
            boolean autostart) throws DBusException {
        if (null == busname) {
            throw new DBusException("Invalid bus name: null");
        }

        if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX)) || busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + busname);
        }

        String unique = dbus.GetNameOwner(busname);

        return getRemoteObject(unique, objectpath, type, autostart);
    }

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param objectpath
     *            The path on which the process is exporting the object.
     * @param type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted or type is not in a package.
     */
    public <I extends DBusInterface> I getRemoteObject(String busname, String objectpath, Class<I> type)
            throws DBusException {
        return getRemoteObject(busname, objectpath, type, true);
    }

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param objectpath
     *            The path on which the process is exporting the object.
     * @param type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @param autostart
     *            Disable/Enable auto-starting of services in response to calls on this object. Default is enabled; when
     *            calling a method with auto-start enabled, if the destination is a well-known name and is not owned the
     *            bus will attempt to start a process to take the name. When disabled an error is returned immediately.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted or type is not in a package.
     */
    @SuppressWarnings("unchecked")
    public <I extends DBusInterface> I getRemoteObject(String busname, String objectpath, Class<I> type,
            boolean autostart) throws DBusException {
        if (null == busname) {
            throw new DBusException("Invalid bus name: null");
        }
        if (null == objectpath) {
            throw new DBusException("Invalid object path: null");
        }
        if (null == type) {
            throw new ClassCastException("Not A DBus Interface");
        }

        if ((!busname.matches(BUSNAME_REGEX) && !busname.matches(CONNID_REGEX)) || busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + busname);
        }

        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }

        if (!DBusInterface.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Interface");
        }

        // don't let people import things which don't have a
        // valid D-Bus interface name
        if (type.getName().equals(type.getSimpleName())) {
            throw new DBusException("DBusInterfaces cannot be declared outside a package");
        }

        RemoteObject ro = new RemoteObject(busname, objectpath, type, autostart);
        I i = (I) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
                type
        }, new RemoteInvocationHandler(this, ro));
        getImportedObjects().put(i, ro);
        return i;
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param source
     *            The source of the signal.
     * @param handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, String source, DBusSigHandler<T> handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!source.matches(CONNID_REGEX) || source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + source);
        }
        removeSigHandler(new DBusMatchRule(type, source, null), handler);
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param source
     *            The source of the signal.
     * @param object
     *            The object emitting the signal.
     * @param handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> type, String source, DBusInterface object,
            DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!source.matches(CONNID_REGEX) || source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + source);
        }
        String objectpath = getImportedObjects().get(object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        removeSigHandler(new DBusMatchRule(type, source, objectpath), handler);
    }

    @Override
    protected <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler)
            throws DBusException {

        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        synchronized (getHandledSignals()) {
            List<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(key);
            if (null != v) {
                v.remove(handler);
                if (0 == v.size()) {
                    getHandledSignals().remove(key);
                    try {
                        dbus.RemoveMatch(rule.toString());
                    } catch (NotConnected exNc) {
                        logger.debug("No connection.", exNc);
                    } catch (DBusExecutionException dbee) {
                        logger.debug("", dbee);
                        throw new DBusException(dbee);
                    }
                }
            }
        }
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type,
     * name and source.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param source
     *            The process which will send the signal. This <b>MUST</b> be a unique bus name and not a well known
     *            name.
     * @param handler
     *            The handler to call when a signal is received.
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void addSigHandler(Class<T> type, String source, DBusSigHandler<T> handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!source.matches(CONNID_REGEX) || source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + source);
        }
        addSigHandler(new DBusMatchRule(type, source, null), (DBusSigHandler<? extends DBusSignal>) handler);
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type,
     * name, source and object.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param type
     *            The signal to watch for.
     * @param source
     *            The process which will send the signal. This <b>MUST</b> be a unique bus name and not a well known
     *            name.
     * @param object
     *            The object from which the signal will be emitted
     * @param handler
     *            The handler to call when a signal is received.
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void addSigHandler(Class<T> type, String source, DBusInterface object,
            DBusSigHandler<T> handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!source.matches(CONNID_REGEX) || source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + source);
        }
        String objectpath = getImportedObjects().get(object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        addSigHandler(new DBusMatchRule(type, source, objectpath), (DBusSigHandler<? extends DBusSignal>) handler);
    }

    @Override
    public <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler)
            throws DBusException {
        try {
            dbus.AddMatch(rule.toString());
        } catch (DBusExecutionException dbee) {
            logger.debug("", dbee);
            throw new DBusException(dbee.getMessage());
        }
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
     * This only disconnects when the last reference to the bus has disconnect called on it or
     * has been destroyed.
     */
    @Override
    public void disconnect() {
    	synchronized (CONNECTIONS) {
	        DBusConnection connection = CONNECTIONS.get(getAddress().getRawAddress());
	        if (connection != null) {
	            if (connection.getConcurrentConnections().get() <= 1) { // one left, this should be ourselfs
	                logger.debug("Disconnecting last remaining DBusConnection");
	                // Set all pending messages to have an error.
	                try {
	                    Error err = new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.Disconnected",
	                            0, "s", new Object[] {
	                                    "Disconnected"
	                            });
	                    cleanupPendingCalls(err, true);

	                    synchronized (getPendingErrorQueue()) {
	                        getPendingErrorQueue().add(err);
	                    }
	                } catch (DBusException dbe) {
	                }
	                CONNECTIONS.remove(getAddress().getRawAddress());

	                super.disconnect();

	            } else {
	            	logger.debug("Still {} connections left, decreasing connection counter", connection.getConcurrentConnections().get() -1);
	                connection.getConcurrentConnections().addAndGet(-1);
	            }
	        }
    	}
    }

    private void cleanupPendingCalls(Error _err, boolean _clearPendingCalls) throws DBusException {

        synchronized (getPendingCalls()) {
            Iterator<Entry<Long, MethodCall>> iter = getPendingCalls().entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Long, MethodCall> entry = iter.next();
                if (entry.getKey() != -1) {
                    MethodCall m = entry.getValue();

                    iter.remove();

                    if (m != null) {
                        m.setReply(_err);
                    }
                }
            }
            if (_clearPendingCalls) {
                getPendingCalls().clear();
            }
        }
    }

    private class SigHandler implements DBusSigHandler<DBusSignal> {
        @Override
        public void handle(DBusSignal s) {
            if (s instanceof org.freedesktop.dbus.interfaces.Local.Disconnected) {
                logger.debug("Handling Disconnected signal from bus");
                try {
                    Error err = new Error("org.freedesktop.DBus.Local", "org.freedesktop.DBus.Local.Disconnected", 0,
                            "s", new Object[] {
                                    "Disconnected"
                            });
                    cleanupPendingCalls(err, false);

                    synchronized (getPendingErrorQueue()) {
                        getPendingErrorQueue().add(err);
                    }
                } catch (DBusException exDb) {
                }
            } else if (s instanceof org.freedesktop.DBus.NameAcquired) {
                busnames.add(((org.freedesktop.DBus.NameAcquired) s).name);
            }
        }
    }

    @Override
    public String getMachineId() {
        return machineId;
    }



    public static enum DBusBusType {
        /**
         * System Bus
         */
        SYSTEM,
        /**
         * Session Bus
         */
        SESSION;
    }
}
