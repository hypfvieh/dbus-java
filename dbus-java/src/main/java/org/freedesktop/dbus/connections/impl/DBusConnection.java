/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.connections.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.SignalTuple;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.IDisconnectAction;
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
    private static final String DBUS_MACHINE_ID_SYS_VAR = "DBUS_MACHINE_ID_LOCATION";

    private List<String>                             busnames;

    private static final ConcurrentMap<String, DBusConnection> CONNECTIONS                = new ConcurrentHashMap<>();
    private DBus                                     dbus;

    private final String                             machineId;

    /** Count how many 'connections' we manage internally.
     * This is required because a {@link DBusConnection} to the same address will always return the same object and
     * the 'real' disconnection should only occur when there is no second/third/whatever connection is left. */
    private final AtomicInteger                      concurrentConnections              = new AtomicInteger(1);

    /**
     * Whether this connection is used in shared mode.
     */
    private final boolean shared;

    /**
     * Connect to the BUS. If a connection already exists to the specified Bus, a reference to it is returned. Will
     * always register our own session to Dbus.
     *
     * @param _address The address of the bus to connect to
     * @throws DBusException If there is a problem connecting to the Bus.
     * @return {@link DBusConnection}
     */
    public static DBusConnection getConnection(String _address) throws DBusException {
        return getConnection(_address, true, true, AbstractConnection.TCP_CONNECT_TIMEOUT);
    }

    /**
     * Connect to the BUS. If a connection already exists to the specified Bus and the shared-flag is true, a reference is returned.
     * Will register our own session to DBus if registerSelf is true (default).
     * A new connection is created every time if shared-flag is false.
     *
     * @param _address The address of the bus to connect to
     * @param _registerSelf register own session in dbus
     * @param _shared use a shared connections
     * @throws DBusException If there is a problem connecting to the Bus.
     * @return {@link DBusConnection}
     */
    public static DBusConnection getConnection(String _address, boolean _registerSelf, boolean _shared)
            throws DBusException {
        return getConnection(_address, _registerSelf, _shared, AbstractConnection.TCP_CONNECT_TIMEOUT);
    }

    /**
     * Connect to the BUS. If a connection already exists to the specified Bus and the shared-flag is true, a reference is returned.
     * Will register our own session to DBus if registerSelf is true (default).
     * A new connection is created every time if shared-flag is false.
     *
     * @param _address The address of the bus to connect to
     * @param _registerSelf register own session in dbus
     * @param _shared use a shared connections
     * @param _timeout connect timeout if this is a TCP socket, 0 will block forever, if this is not a TCP socket this value is ignored
     * @throws DBusException If there is a problem connecting to the Bus.
     * @return {@link DBusConnection}
     */
    public static DBusConnection getConnection(String _address, boolean _registerSelf, boolean _shared, int _timeout)
            throws DBusException {

        // CONNECTIONS.getOrDefault(address, defaultValue)
        if (_shared) {
            synchronized (CONNECTIONS) {
                DBusConnection c = CONNECTIONS.get(_address);
                if (c != null) {
                    c.concurrentConnections.incrementAndGet();
                    return c;
                } else {
                    c = new DBusConnection(_address, _shared, _registerSelf, getDbusMachineId(), _timeout);
                    // do not increment connection counter here, it always starts at 1 on new objects!
                    // c.getConcurrentConnections().incrementAndGet();
                    CONNECTIONS.put(_address, c);
                    return c;
                }
            }
        } else {
            return new DBusConnection(_address, _shared, _registerSelf, getDbusMachineId(), _timeout);
        }
    }

    private static DBusConnection getConnection(Supplier<String> _addressGenerator, boolean _registerSelf, boolean _shared, int _timeout) throws DBusException {
        if (_addressGenerator == null) {
            throw new DBusException("Invalid address generator");
        }
        String address = _addressGenerator.get();
        if (address == null) {
            throw new DBusException("null is not a valid DBUS address");
        }
        return getConnection(address, _registerSelf, _shared, _timeout);
    }

    /**
     * Connect to DBus.
     * If a connection already exists to the specified Bus, a reference to it is returned.
     *
     * @param _bustype The Bus to connect to.

     * @return {@link DBusConnection}
     *
     * @throws DBusException If there is a problem connecting to the Bus.
     *
     */
    public static DBusConnection getConnection(DBusBusType _bustype) throws DBusException {
        return getConnection(_bustype, true, AbstractConnection.TCP_CONNECT_TIMEOUT);
    }

    /**
     * Connect to DBus using a new connection even if there is already a connection established.
     *
     * @param _bustype The Bus to connect to.
     *
     * @return {@link DBusConnection}
     *
     * @throws DBusException If there is a problem connecting to the Bus.
     *
     */
    public static DBusConnection newConnection(DBusBusType _bustype) throws DBusException {
        return getConnection(_bustype, false, AbstractConnection.TCP_CONNECT_TIMEOUT);
    }


    /**
     * Connect to the BUS.
     * If a connection to the specified Bus already exists and shared-flag is true, a reference to it is returned.
     * Otherwise a new connection will be created.
     *
     * @param _bustype The Bus to connect to.
     * @param _shared use shared connection
     * @param _timeout connect timeout if this is a TCP socket, 0 will block forever, if this is not a TCP socket this value is ignored
     *
     * @return {@link DBusConnection}
     *
     * @throws DBusException If there is a problem connecting to the Bus.
     *
     */
    public static DBusConnection getConnection(DBusBusType _bustype, boolean _shared, int _timeout) throws DBusException {
        switch (_bustype) {
            case SYSTEM:
                DBusConnection systemConnection = getConnection(() -> {
                    String bus = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
                    if (bus == null) {
                        bus = DEFAULT_SYSTEM_BUS_ADDRESS;
                    }
                    return bus;
                }, true, _shared, _timeout);
                return systemConnection;
            case SESSION:
                DBusConnection sessionConnection = getConnection(() -> {
                    String s = null;

                    // MacOS support: e.g DBUS_LAUNCHD_SESSION_BUS_SOCKET=/private/tmp/com.apple.launchd.4ojrKe6laI/unix_domain_listener
                    if (SystemUtil.isMacOs()) {
                        s = "unix:path=" + System.getenv("DBUS_LAUNCHD_SESSION_BUS_SOCKET");

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

                            // sometimes (e.g. Ubuntu 18.04) the returned address is wrapped in single quotes ('), we have to remove them
                            if (sessionAddress.matches("^'[^']+'$")) {
                                sessionAddress = sessionAddress.replaceFirst("^'([^']+)'$", "$1");
                            }
                            
                            return sessionAddress;
                        } catch (DBusException _ex) {
                            throw new RuntimeException("Cannot Resolve Session Bus Address", _ex);
                        }
                    }

                    return s;

                }, true, _shared, _timeout);

                return sessionConnection;
            default:
                throw new DBusException("Invalid Bus Type: " + _bustype);
        }

    }

    private AtomicInteger getConcurrentConnections() {
        return concurrentConnections;
    }

    /**
     * Extracts the machine-id usually found in /var/lib/dbus/machine-id.
     * Use system variable DBUS_MACHINE_ID_LOCATION to use other location
     *
     * @return machine-id string, never null
     * @throws DBusException if machine-id could not be found
     */
    public static String getDbusMachineId() throws DBusException {
        if (isWindows()) {
            return getDbusMachineIdOnWindows();
        }
    	File uuidfile = determineMachineIdFile();
        String uuid = FileIoUtil.readFileToString(uuidfile);
        if (StringUtil.isEmpty(uuid)) {
            throw new DBusException("Cannot Resolve Session Bus Address: MachineId file is empty.");
        }

        return uuid;
    }

	private static File determineMachineIdFile() throws DBusException {
		List<String> locationPriorityList = Arrays.asList(System.getenv(DBUS_MACHINE_ID_SYS_VAR),
				"/var/lib/dbus/machine-id", "/usr/local/var/lib/dbus/machine-id", "/etc/machine-id");
		return locationPriorityList.stream()
				.filter(s -> s != null)
				.map(s -> new File(s))
				.filter(f -> f.exists())
				.findFirst()
				.orElseThrow(() -> new DBusException("Cannot Resolve Session Bus Address: MachineId file can not be found"));
	}
	
    private static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName == null ? false : osName.toLowerCase().startsWith("windows");
    }
	
	private static String getDbusMachineIdOnWindows() {
	    // we create a fake id on windows
	    return String.format("%s@%s", SystemUtil.getCurrentUser(), SystemUtil.getHostName());
	}

    private DBusConnection(String _address, boolean _shared, boolean _registerSelf, String _machineId, int timeout) throws DBusException {
        super(_address, timeout);
        busnames = new ArrayList<>();
        machineId = _machineId;
        shared = _shared;
        // start listening for calls
        listen();

        // register disconnect handlers
        DBusSigHandler<?> h = new SigHandler();
        addSigHandlerWithoutMatch(org.freedesktop.dbus.interfaces.Local.Disconnected.class, h);
        addSigHandlerWithoutMatch(org.freedesktop.DBus.NameAcquired.class, h);

        // register ourselves if not disabled
        if (_registerSelf) {
            dbus = getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            try {
                busnames.add(dbus.Hello());
            } catch (DBusExecutionException dbee) {
                logger.debug("", dbee);
                throw new DBusException(dbee.getMessage());
            }
        }
    }

    protected DBusInterface dynamicProxy(String _source, String _path) throws DBusException {
        logger.debug("Introspecting {} on {} for dynamic proxy creation", _path, _source);
        try {
            Introspectable intro = getRemoteObject(_source, _path, Introspectable.class);
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

            RemoteObject ro = new RemoteObject(_source, _path, null, false);
            DBusInterface newi = (DBusInterface) Proxy.newProxyInstance(ifcs.get(0).getClassLoader(),
                    ifcs.toArray(new Class[0]), new RemoteInvocationHandler(this, ro));
            getImportedObjects().put(newi, ro);
            return newi;
        } catch (Exception e) {
            logger.debug("", e);
            throw new DBusException(
                    String.format("Failed to create proxy object for %s exported by %s. Reason: %s", _path,
                            _source, e.getMessage()));
        }
    }

    @Override
    public DBusInterface getExportedObject(String _source, String _path) throws DBusException {
        ExportedObject o = null;
        synchronized (getExportedObjects()) {
            o = getExportedObjects().get(_path);
        }
        if (null != o && null == o.getObject().get()) {
            unExportObject(_path);
            o = null;
        }
        if (null != o) {
            return o.getObject().get();
        }
        if (null == _source) {
            throw new DBusException("Not an object exported by this connection and no remote specified");
        }
        return dynamicProxy(_source, _path);
    }

    /**
     * Release a bus name. Releases the name so that other people can use it
     *
     * @param _busname
     *            The name to release. MUST be in dot-notation like "org.freedesktop.local"
     * @throws DBusException
     *             If the busname is incorrectly formatted.
     */
    public void releaseBusName(String _busname) throws DBusException {
        if (!_busname.matches(BUSNAME_REGEX) || _busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name");
        }
        try {
            dbus.ReleaseName(_busname);
        } catch (DBusExecutionException dbee) {
            logger.debug("", dbee);
            throw new DBusException(dbee.getMessage());
        }

        synchronized (this.busnames) {
            this.busnames.remove(_busname);
        }
    }

    /**
     * Request a bus name. Request the well known name that this should respond to on the Bus.
     *
     * @param _busname
     *            The name to respond to. MUST be in dot-notation like "org.freedesktop.local"
     * @throws DBusException
     *             If the register name failed, or our name already exists on the bus. or if busname is incorrectly
     *             formatted.
     */
    public void requestBusName(String _busname) throws DBusException {
        if (!_busname.matches(BUSNAME_REGEX) || _busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name");
        }

        UInt32 rv;
        try {
            rv = dbus.RequestName(_busname,
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
        synchronized (this.busnames) {
            this.busnames.add(_busname);
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
        Set<String> names = new TreeSet<>();
        names.addAll(busnames);
        return names.toArray(new String[0]);
    }

    public <I extends DBusInterface> I getPeerRemoteObject(String _busname, String _objectpath, Class<I> _type)
            throws DBusException {
        return getPeerRemoteObject(_busname, _objectpath, _type, true);
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
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.$
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted.
     */
    public DBusInterface getPeerRemoteObject(String _busname, String _objectpath) throws DBusException {
        if (null == _busname) {
            throw new DBusException("Invalid bus name: null");
        }

        if ((!_busname.matches(BUSNAME_REGEX) && !_busname.matches(CONNID_REGEX)) || _busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _busname);
        }

        String unique = dbus.GetNameOwner(_busname);

        return dynamicProxy(unique, _objectpath);
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
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted.
     */
    public DBusInterface getRemoteObject(String _busname, String _objectpath) throws DBusException {
        if (null == _busname) {
            throw new DBusException("Invalid bus name: null");
        }
        if (null == _objectpath) {
            throw new DBusException("Invalid object path: null");
        }

        if ((!_busname.matches(BUSNAME_REGEX) && !_busname.matches(CONNID_REGEX)) || _busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _busname);
        }

        if (!_objectpath.matches(OBJECT_REGEX) || _objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + _objectpath);
        }

        return dynamicProxy(_busname, _objectpath);
    }

    /**
     * Return a reference to a remote object. This method will resolve the well known name (if given) to a unique bus
     * name when you call it. This means that if a well known name is released by one process and acquired by another
     * calls to objects gained from this method will continue to operate on the original process.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.$
     * @param _type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @param _autostart
     *            Disable/Enable auto-starting of services in response to calls on this object. Default is enabled; when
     *            calling a method with auto-start enabled, if the destination is a well-known name and is not owned the
     *            bus will attempt to start a process to take the name. When disabled an error is returned immediately.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted or type is not in a package.
     */
    public <I extends DBusInterface> I getPeerRemoteObject(String _busname, String _objectpath, Class<I> _type,
            boolean _autostart) throws DBusException {
        if (null == _busname) {
            throw new DBusException("Invalid bus name: null");
        }

        if ((!_busname.matches(BUSNAME_REGEX) && !_busname.matches(CONNID_REGEX)) || _busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _busname);
        }

        String unique = dbus.GetNameOwner(_busname);

        return getRemoteObject(unique, _objectpath, _type, _autostart);
    }

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.
     * @param _type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @return A reference to a remote object.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusInterface
     * @throws DBusException
     *             If busname or objectpath are incorrectly formatted or type is not in a package.
     */
    public <I extends DBusInterface> I getRemoteObject(String _busname, String _objectpath, Class<I> _type)
            throws DBusException {
        return getRemoteObject(_busname, _objectpath, _type, true);
    }

    /**
     * Return a reference to a remote object. This method will always refer to the well known name (if given) rather
     * than resolving it to a unique bus name. In particular this means that if a process providing the well known name
     * disappears and is taken over by another process proxy objects gained by this method will make calls on the new
     * proccess.
     *
     * @param <I>
     *            class extending {@link DBusInterface}
     * @param _busname
     *            The bus name to connect to. Usually a well known bus name name in dot-notation (such as
     *            "org.freedesktop.local") or may be a DBus address such as ":1-16".
     * @param _objectpath
     *            The path on which the process is exporting the object.
     * @param _type
     *            The interface they are exporting it on. This type must have the same full class name and exposed
     *            method signatures as the interface the remote object is exporting.
     * @param _autostart
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
    public <I extends DBusInterface> I getRemoteObject(String _busname, String _objectpath, Class<I> _type,
            boolean _autostart) throws DBusException {
        if (null == _busname) {
            throw new DBusException("Invalid bus name: null");
        }
        if (null == _objectpath) {
            throw new DBusException("Invalid object path: null");
        }
        if (null == _type) {
            throw new ClassCastException("Not A DBus Interface");
        }

        if ((!_busname.matches(BUSNAME_REGEX) && !_busname.matches(CONNID_REGEX)) || _busname.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _busname);
        }

        if (!_objectpath.matches(OBJECT_REGEX) || _objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + _objectpath);
        }

        if (!DBusInterface.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Interface");
        }

        // don't let people import things which don't have a
        // valid D-Bus interface name
        if (_type.getName().equals(_type.getSimpleName())) {
            throw new DBusException("DBusInterfaces cannot be declared outside a package");
        }

        RemoteObject ro = new RemoteObject(_busname, _objectpath, _type, _autostart);
        I i = (I) Proxy.newProxyInstance(_type.getClassLoader(), new Class[] {
                _type
        }, new RemoteInvocationHandler(this, ro));
        getImportedObjects().put(i, ro);
        return i;
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _source
     *            The source of the signal.
     * @param _handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> _type, String _source, DBusSigHandler<T> _handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (_source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!_source.matches(CONNID_REGEX) || _source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _source);
        }
        removeSigHandler(new DBusMatchRule(_type, _source, null), _handler);
    }

    /**
     * Remove a Signal Handler. Stops listening for this signal.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _source
     *            The source of the signal.
     * @param _object
     *            The object emitting the signal.
     * @param _handler
     *            the handler
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void removeSigHandler(Class<T> _type, String _source, DBusInterface _object,
            DBusSigHandler<T> _handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (_source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!_source.matches(CONNID_REGEX) || _source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _source);
        }
        String objectpath = getImportedObjects().get(_object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        removeSigHandler(new DBusMatchRule(_type, _source, objectpath), _handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T extends DBusSignal> void removeSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler)
            throws DBusException {

        SignalTuple key = new SignalTuple(_rule.getInterface(), _rule.getMember(), _rule.getObject(), _rule.getSource());
        Queue<DBusSigHandler<? extends DBusSignal>> dbusSignalList = getHandledSignals().get(key);
        
        if (null != dbusSignalList) {
            dbusSignalList.remove(_handler);
            if (dbusSignalList.isEmpty()) {
                getHandledSignals().remove(key);
                try {
                    dbus.RemoveMatch(_rule.toString());
                } catch (NotConnected exNc) {
                    logger.debug("No connection.", exNc);
                } catch (DBusExecutionException dbee) {
                    logger.debug("", dbee);
                    throw new DBusException(dbee);
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
     * @param _type
     *            The signal to watch for.
     * @param _source
     *            The process which will send the signal. This <b>MUST</b> be a unique bus name and not a well known
     *            name.
     * @param _handler
     *            The handler to call when a signal is received.
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void addSigHandler(Class<T> _type, String _source, DBusSigHandler<T> _handler)
            throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (_source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!_source.matches(CONNID_REGEX) || _source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _source);
        }
        addSigHandler(new DBusMatchRule(_type, _source, null), (DBusSigHandler<? extends DBusSignal>) _handler);
    }

    /**
     * Add a Signal Handler. Adds a signal handler to call when a signal is received which matches the specified type,
     * name, source and object.
     *
     * @param <T>
     *            class extending {@link DBusSignal}
     * @param _type
     *            The signal to watch for.
     * @param _source
     *            The process which will send the signal. This <b>MUST</b> be a unique bus name and not a well known
     *            name.
     * @param _object
     *            The object from which the signal will be emitted
     * @param _handler
     *            The handler to call when a signal is received.
     * @throws DBusException
     *             If listening for the signal on the bus failed.
     * @throws ClassCastException
     *             If type is not a sub-type of DBusSignal.
     */
    public <T extends DBusSignal> void addSigHandler(Class<T> _type, String _source, DBusInterface _object,
            DBusSigHandler<T> _handler) throws DBusException {
        if (!DBusSignal.class.isAssignableFrom(_type)) {
            throw new ClassCastException("Not A DBus Signal");
        }
        if (_source.matches(BUSNAME_REGEX)) {
            throw new DBusException(
                    "Cannot watch for signals based on well known bus name as source, only unique names.");
        }
        if (!_source.matches(CONNID_REGEX) || _source.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid bus name: " + _source);
        }
        String objectpath = getImportedObjects().get(_object).getObjectPath();
        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }
        addSigHandler(new DBusMatchRule(_type, _source, objectpath), (DBusSigHandler<? extends DBusSignal>) _handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends DBusSignal> void addSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler)
            throws DBusException {

        Objects.requireNonNull(_rule, "Match rule cannot be null");
        Objects.requireNonNull(_handler, "Handler cannot be null");

        AtomicBoolean addMatch = new AtomicBoolean(false); // flag to perform action if this is a new signal key
        
        SignalTuple key = new SignalTuple(_rule.getInterface(), _rule.getMember(), _rule.getObject(), _rule.getSource());

        Queue<DBusSigHandler<? extends DBusSignal>> dbusSignalList = 
            getHandledSignals().computeIfAbsent(key, v -> {
                Queue<DBusSigHandler<? extends DBusSignal>> signalList  = new ConcurrentLinkedQueue<>();
                addMatch.set(true);
                return signalList;
            });

        // add handler to signal list
        dbusSignalList.add(_handler);

        // add match rule if this rule is new
        if (addMatch.get()) {
            try {
                dbus.AddMatch(_rule.toString());
            } catch (DBusExecutionException dbee) {
                logger.debug("Cannot add match rule: " + _rule.toString(), dbee);
                throw new DBusException("Cannot add match rule.", dbee);
            }
        }
    }

    /**
     * Disconnect from the Bus.
     * If this is a shared connection, it only disconnects when the last reference to the bus has called disconnect.
     * If this is not a shared connection, disconnect will close the connection instantly.
     */
    @Override
    public void disconnect() {
        if (!isConnected()) { // already disconnected
            return;
        }

        // if this is a shared connection, keep track of disconnect calls
        if (shared) {

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

        } else { // this is a standalone non-shared session, disconnect directly using super's implementation
        	IDisconnectAction beforeDisconnectAction = () -> {

        	    // get all busnames from the list which matches the usual pattern
        	    // this is required as the list also contains internal names like ":1.11"
        	    // it is also required to put the results in a new list, otherwise we would get a
        	    // concurrent modification exception later (calling releaseBusName() will modify the busnames List)
        	    synchronized (busnames) {
                    List<String> lBusNames = busnames.stream()
            	        .filter(busName -> busName != null && !(!busName.matches(BUSNAME_REGEX) || busName.length() > MAX_NAME_LENGTH))
            	        .collect(Collectors.toList());
    
    
                    lBusNames.forEach(busName -> {
                            try {
                                releaseBusName(busName);
    
                            } catch (DBusException _ex) {
                                logger.error("Error while releasing busName '" + busName + "'.", _ex);
                            }
    
            	        });
        	    }
                
                // remove all exported objects before disconnecting
                Map<String, ExportedObject> exportedObjects = getExportedObjects();
                synchronized (exportedObjects) {
                    List<String> exportedKeys = exportedObjects.keySet().stream().filter(f -> f != null).collect(Collectors.toList());
                    for (String key : exportedKeys) {
                        unExportObject(key);
                    }
                }

        	};

            super.disconnect(beforeDisconnectAction, null);
        }
    }

    /**
     * Same as disconnect.
     */
    @Override
	public void close() throws IOException {
		disconnect();
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

	/**
	 * {@inheritDoc}
	 */
    @Override
    public void removeGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException {
        SignalTuple key = new SignalTuple(_rule.getInterface(), _rule.getMember(), _rule.getObject(), _rule.getSource());
        Queue<DBusSigHandler<DBusSignal>> genericSignalsList = getGenericHandledSignals().get(key);
        if (null != genericSignalsList) {
            genericSignalsList.remove(_handler);
            if (genericSignalsList.isEmpty()) {
                getGenericHandledSignals().remove(key);
                try {
                    dbus.RemoveMatch(_rule.toString());
                } catch (NotConnected exNc) {
                    logger.debug("No connection.", exNc);
                } catch (DBusExecutionException dbee) {
                    logger.debug("", dbee);
                    throw new DBusException(dbee);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException {
        SignalTuple key = new SignalTuple(_rule.getInterface(), _rule.getMember(), _rule.getObject(), _rule.getSource());
        
        AtomicBoolean addMatch = new AtomicBoolean(false); // flag to perform action if this is a new signal key

        Queue<DBusSigHandler<DBusSignal>> genericSignalsList = 
                getGenericHandledSignals().computeIfAbsent(key, v -> {
                    Queue<DBusSigHandler<DBusSignal>> signalsList = new ConcurrentLinkedQueue<>();
                    addMatch.set(true);

                    return signalsList;
                });

        genericSignalsList.add(_handler);

        if (addMatch.get()) {
            try {
                dbus.AddMatch(_rule.toString());
            } catch (DBusExecutionException dbee) {
                logger.debug("", dbee);
                throw new DBusException(dbee.getMessage());
            }
        }
    }

    private class SigHandler implements DBusSigHandler<DBusSignal> {
        @Override
        public void handle(DBusSignal _signal) {
            if (_signal instanceof org.freedesktop.dbus.interfaces.Local.Disconnected) {
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
            } else if (_signal instanceof org.freedesktop.DBus.NameAcquired) {
                synchronized (busnames) {
                    busnames.add(((org.freedesktop.DBus.NameAcquired) _signal).name);
                }
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
