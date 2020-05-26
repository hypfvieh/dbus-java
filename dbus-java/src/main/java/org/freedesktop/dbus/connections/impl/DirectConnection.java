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
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.freedesktop.Hexdump;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.SignalTuple;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.FreeBSDHelper;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.ExportedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.util.StringUtil;

/** Handles a peer to peer connection between two applications withou a bus daemon.
 * <p>
 * Signal Handlers and method calls from remote objects are run in their own threads, you MUST handle the concurrency issues.
 * </p>
 */
public class DirectConnection extends AbstractConnection {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String machineId;
    
    /**
     * Create a direct connection to another application.
     * @param address The address to connect to. This is a standard D-Bus address, except that the additional parameter 'listen=true' should be added in the application which is creating the socket.
     * @throws DBusException on error
     */
    public DirectConnection(String address) throws DBusException {
        this(address, AbstractConnection.TCP_CONNECT_TIMEOUT);
    }
    
    /**
    * Create a direct connection to another application.
    * @param address The address to connect to. This is a standard D-Bus address, except that the additional parameter 'listen=true' should be added in the application which is creating the socket.
    * @param timeout the timeout set for the underlying socket. 0 will block forever on the underlying socket. 
    * @throws DBusException on error
    */
    public DirectConnection(String address, int timeout) throws DBusException {
        super(address, timeout);
        machineId = createMachineId();
        if (!getAddress().isServer()) {
            super.listen();
        }
    }

    /**
     * Use this method when running on server side.
     * Call will block.
     */
    @Override
    public void listen() {
        if (getAddress().isServer()) {
            super.listen();
        }
    }



    private String createMachineId() {
        String ascii;

        try {
            ascii = Hexdump.toAscii(MessageDigest.getInstance("MD5").digest(InetAddress.getLocalHost().getHostName().getBytes()));
            return ascii;
        } catch (NoSuchAlgorithmException | UnknownHostException _ex) {
        }

        return StringUtil.randomString(32);
    }

    /**
    * Creates a bus address for a randomly generated tcp port.
    * @return a random bus address.
    */
    public static String createDynamicTCPSession() {
        String address = "tcp:host=localhost";
        int port;
        try {
            ServerSocket s = new ServerSocket();
            s.bind(null);
            port = s.getLocalPort();
            s.close();
        } catch (Exception e) {
            Random r = new Random();
            port = 32768 + (Math.abs(r.nextInt()) % 28232);
        }
        address += ",port=" + port;
        address += ",guid=" + TransportFactory.genGUID();
        LoggerFactory.getLogger(DirectConnection.class).debug("Created Session address: {}", address);
        return address;
    }

    /**
    * Creates a bus address for a randomly generated abstract unix socket.
    * @return a random bus address.
    */
    public static String createDynamicSession() {
        String address = "unix:";
        String path = "/tmp/dbus-XXXXXXXXXX";
        Random r = new Random();
        do {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 10; i++) {
                sb.append((char) ((Math.abs(r.nextInt()) % 26) + 65));
            }
            path = path.replaceAll("..........$", sb.toString());
            LoggerFactory.getLogger(DirectConnection.class).trace("Trying path {}", path);
        } while ((new File(path)).exists());
        if (FreeBSDHelper.isFreeBSD()) {
            address += "path=" + path;
        } else {
            address += "abstract=" + path;
        }
        address += ",guid=" + TransportFactory.genGUID();
        LoggerFactory.getLogger(DirectConnection.class).debug("Created Session address: {}", address);
        return address;
    }

    DBusInterface dynamicProxy(String path) throws DBusException {
        try {
            Introspectable intro = getRemoteObject(path, Introspectable.class);
            String data = intro.Introspect();
            String[] tags = data.split("[<>]");
            List<String> ifaces = new ArrayList<>();
            for (String tag : tags) {
                if (tag.startsWith("interface")) {
                    ifaces.add(tag.replaceAll("^interface *name *= *['\"]([^'\"]*)['\"].*$", "$1"));
                }
            }
            List<Class<? extends Object>> ifcs = new ArrayList<>();
            for (String iface : ifaces) {
                int j = 0;
                while (j >= 0) {
                    try {
                        ifcs.add(Class.forName(iface));
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

            if (ifcs.size() == 0) {
                throw new DBusException("Could not find an interface to cast to");
            }

            RemoteObject ro = new RemoteObject(null, path, null, false);
            DBusInterface newi = (DBusInterface) Proxy.newProxyInstance(ifcs.get(0).getClassLoader(), ifcs.toArray(new Class[0]), new RemoteInvocationHandler(this, ro));
            getImportedObjects().put(newi, ro);
            return newi;
        } catch (Exception e) {
            logger.debug("", e);
            throw new DBusException(String.format("Failed to create proxy object for %s; reason: %s.", path, e.getMessage()));
        }
    }

    DBusInterface getExportedObject(String path) throws DBusException {
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
        return dynamicProxy(path);
    }

    /**
       * Return a reference to a remote object.
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       *
       * This method will use bus introspection to determine the interfaces on a remote object and so
       * <b>may block</b> and <b>may fail</b>. The resulting proxy object will, however, be castable
       * to any interface it implements. It will also autostart the process if applicable. Also note
       * that the resulting proxy may fail to execute the correct method with overloaded methods
       * and that complex types may fail in interesting ways. Basically, if something odd happens,
       * try specifying the interface explicitly.
       *
       * @param objectpath The path on which the process is exporting the object.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
    public DBusInterface getRemoteObject(String objectpath) throws DBusException {
        if (null == objectpath) {
            throw new DBusException("Invalid object path: null");
        }

        if (!objectpath.matches(OBJECT_REGEX) || objectpath.length() > MAX_NAME_LENGTH) {
            throw new DBusException("Invalid object path: " + objectpath);
        }

        return dynamicProxy(objectpath);
    }

    /**
       * Return a reference to a remote object.
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       * @param objectpath The path on which the process is exporting the object.
       * @param type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @param <T> class which extends DBusInterface
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted or type is not in a package.
    */
    public <T extends DBusInterface> T getRemoteObject(String objectpath, Class<T> type) throws DBusException {
        if (null == objectpath) {
            throw new DBusException("Invalid object path: null");
        }
        if (null == type) {
            throw new ClassCastException("Not A DBus Interface");
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

        RemoteObject ro = new RemoteObject(null, objectpath, type, false);

        @SuppressWarnings("unchecked")
        T i = (T) Proxy.newProxyInstance(type.getClassLoader(),
                new Class[] { type }, new RemoteInvocationHandler(this, ro));

        getImportedObjects().put(i, ro);

        return i;
    }

    @Override
    protected <T extends DBusSignal> void removeSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException {
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        Queue<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(key);
        if (null != v) {
            v.remove(handler);
            if (0 == v.size()) {
                getHandledSignals().remove(key);
            }
        }
    }

    @Override
    protected <T extends DBusSignal> void addSigHandler(DBusMatchRule rule, DBusSigHandler<T> handler) throws DBusException {
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
       
        Queue<DBusSigHandler<? extends DBusSignal>> v = 
                getHandledSignals().computeIfAbsent(key, val -> {
                    Queue<DBusSigHandler<? extends DBusSignal>> l = new ConcurrentLinkedQueue<>();
                    return l;
                });
    
        v.add(handler);
    }

    @Override
    protected void removeGenericSigHandler(DBusMatchRule rule, DBusSigHandler<DBusSignal> handler) throws DBusException {
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        Queue<DBusSigHandler<DBusSignal>> v = getGenericHandledSignals().get(key);
        if (null != v) {
            v.remove(handler);
            if (0 == v.size()) {
                getGenericHandledSignals().remove(key);
            }
        }
    }

    @Override
    protected void addGenericSigHandler(DBusMatchRule rule, DBusSigHandler<DBusSignal> handler) throws DBusException {
        SignalTuple key = new SignalTuple(rule.getInterface(), rule.getMember(), rule.getObject(), rule.getSource());
        Queue<DBusSigHandler<DBusSignal>> v = 
                getGenericHandledSignals().computeIfAbsent(key, val -> {
                    Queue<DBusSigHandler<DBusSignal>> l = new ConcurrentLinkedQueue<>();
                    return l;
                });

        v.add(handler);
    }

    @Override
    public DBusInterface getExportedObject(String source, String path) throws DBusException {
        return getExportedObject(path);
    }

    @Override
    public String getMachineId() {
       return machineId;
    }
}
