/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.bin;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.DBus;
import org.freedesktop.Hexdump;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.MatchRuleInvalid;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.spi.InputStreamMessageReader;
import org.freedesktop.dbus.spi.OutputStreamMessageWriter;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.unixsocket.UnixSocket;

/**
 * A replacement DBusDaemon
 */
public class DBusDaemon extends Thread implements Closeable {
    public static final int     QUEUE_POLL_WAIT = 500;

    private static final Logger LOGGER          = LoggerFactory.getLogger(DBusDaemon.class);

    static class Connstruct {
        // CHECKSTYLE:OFF
        public UnixSocket    usock;
        public Socket        tsock;
        public InputStreamMessageReader min;
        public OutputStreamMessageWriter mout;
        public String        unique;
        // CHECKSTYLE:ON

        Connstruct(UnixSocket sock) throws IOException {
            this.usock = sock;
            min = new InputStreamMessageReader(sock.getInputStream());
            mout = new OutputStreamMessageWriter(sock.getOutputStream());
        }

        Connstruct(Socket sock) throws IOException {
            this.tsock = sock;
            min = new InputStreamMessageReader(sock.getInputStream());
            mout = new OutputStreamMessageWriter(sock.getOutputStream());
        }

        @Override
        public String toString() {
            return null == unique ? ":?-?" : unique;
        }
    }

    static class MagicMap<A, B> {
        private final Logger          logger = LoggerFactory.getLogger(getClass());

        private Map<A, LinkedList<B>> m;
        private LinkedList<A>         q;
        private String                name;

        MagicMap(String _name) {
            m = new HashMap<>();
            q = new LinkedList<>();
            this.name = _name;
        }

        public A head() {
            return q.getFirst();
        }

        public void putFirst(A a, B b) {
            logger.debug("<{}> Queueing {{} => {}}", name, a, b);

            if (m.containsKey(a)) {
                m.get(a).add(b);
            } else {
                LinkedList<B> l = new LinkedList<>();
                l.add(b);
                m.put(a, l);
            }
            q.addFirst(a);
        }

        public void putLast(A a, B b) {
            logger.debug("<{}> Queueing {{} => {}}", name, a, b);

            if (m.containsKey(a)) {
                m.get(a).add(b);
            } else {
                LinkedList<B> l = new LinkedList<>();
                l.add(b);
                m.put(a, l);
            }
            q.addLast(a);
        }

        public List<B> remove(A a) {
            logger.debug("<{}> Removing {{}}", name, a);

            q.remove(a);
            return m.remove(a);
        }

        public int size() {
            return q.size();
        }
    }

    public class DBusServer extends Thread implements DBus, Introspectable, Peer {

        private final String machineId;

        public DBusServer() {
            setName("Server");
            String ascii;
            try {
                ascii = Hexdump.toAscii(MessageDigest.getInstance("MD5").digest(InetAddress.getLocalHost().getHostName().getBytes()));
            } catch (NoSuchAlgorithmException | UnknownHostException _ex) {
                ascii = this.hashCode() + "";
            }

            machineId = ascii;
        }



        // CHECKSTYLE:OFF
        public Connstruct c;
        public Message    m;
        // CHECKSTYLE:ON

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String Hello() {

            LOGGER.debug("enter");

            synchronized (c) {
                if (null != c.unique) {
                    throw new org.freedesktop.dbus.errors.AccessDenied("Connection has already sent a Hello message");
                }
                synchronized (uniqueLock) {
                    c.unique = ":1." + (++nextUnique);
                }
            }
            synchronized (names) {
                names.put(c.unique, c);
            }

            LOGGER.info("Client {} registered", c.unique);

            try {
                send(c, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameAcquired", "s", c.unique));
                DBusSignal s = new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameOwnerChanged", "sss", c.unique, "", c.unique);
                send(null, s);
            } catch (DBusException dbe) {
                LOGGER.debug("", dbe);
            }

            LOGGER.debug("exit");

            return c.unique;
        }

        @Override
        public String[] ListNames() {
            LOGGER.debug("enter");
            String[] ns;
            synchronized (names) {
                Set<String> nss = names.keySet();
                ns = nss.toArray(new String[0]);
            }

            LOGGER.debug("exit");

            return ns;
        }

        @Override
        public boolean NameHasOwner(String name) {

            LOGGER.debug("enter");

            boolean rv;
            synchronized (names) {
                rv = names.containsKey(name);
            }

            LOGGER.debug("exit");

            return rv;
        }

        @Override
        public String GetNameOwner(String name) {
            LOGGER.debug("enter");
            Connstruct owner = names.get(name);
            String o;
            if (null == owner) {
                o = "";
            } else {
                o = owner.unique;
            }

            LOGGER.debug("exit");

            return o;
        }

        @Override
        public UInt32 GetConnectionUnixUser(String connection_name) {
            LOGGER.debug("enter");
            LOGGER.debug("exit");
            return new UInt32(0);
        }

        @Override
        public UInt32 StartServiceByName(String name, UInt32 flags) {

            LOGGER.debug("enter");

            LOGGER.debug("exit");

            return new UInt32(0);
        }

        @Override
        public UInt32 RequestName(String name, UInt32 flags) {
            LOGGER.debug("enter");

            boolean exists = false;
            synchronized (names) {
                if (!(exists = names.containsKey(name))) {
                    names.put(name, c);
                }
            }

            int rv;
            if (exists) {
                rv = DBus.DBUS_REQUEST_NAME_REPLY_EXISTS;
            } else {

                LOGGER.info("Client {} acquired name {}", c.unique, name);

                rv = DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER;
                try {
                    send(c, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameAcquired", "s", name));
                    send(null, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameOwnerChanged", "sss", name, "", c.unique));
                } catch (DBusException dbe) {
                    LOGGER.debug("", dbe);
                }
            }

            LOGGER.debug("exit");

            return new UInt32(rv);
        }

        @Override
        public UInt32 ReleaseName(String name) {
            LOGGER.debug("enter");

            boolean exists = false;
            synchronized (names) {
                if (names.containsKey(name) && names.get(name).equals(c)) {
                	exists = names.remove(name) != null;
                }
            }

            int rv;
            if (!exists) {
                rv = DBus.DBUS_RELEASE_NAME_REPLY_NON_EXISTANT;
            } else {
                LOGGER.info("Client {} acquired name {}", c.unique, name);
                rv = DBus.DBUS_RELEASE_NAME_REPLY_RELEASED;
                try {
                    send(c, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameLost", "s", name));
                    send(null, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameOwnerChanged", "sss", name, c.unique, ""));
                } catch (DBusException dbe) {
                    LOGGER.debug("", dbe);
                }
            }

            LOGGER.debug("exit");

            return new UInt32(rv);
        }

        @Override
        public void AddMatch(String matchrule) throws MatchRuleInvalid {

            LOGGER.debug("enter");

            LOGGER.trace("Adding match rule: {}", matchrule);

            synchronized (sigrecips) {
                if (!sigrecips.contains(c)) {
                    sigrecips.add(c);
                }
            }

            LOGGER.debug("exit");

            return;
        }

        @Override
        public void RemoveMatch(String matchrule) throws MatchRuleInvalid {

            LOGGER.debug("enter");

            LOGGER.trace("Removing match rule: {}", matchrule);

            LOGGER.debug("exit");

            return;
        }

        @Override
        public String[] ListQueuedOwners(String name) {

            LOGGER.debug("enter");

            LOGGER.debug("exit");

            return new String[0];
        }

        @Override
        public UInt32 GetConnectionUnixProcessID(String connection_name) {

            LOGGER.debug("enter");

            LOGGER.debug("exit");

            return new UInt32(0);
        }

        @Override
        public Byte[] GetConnectionSELinuxSecurityContext(String a) {
            LOGGER.debug("enter");

            LOGGER.debug("exit");

            return new Byte[0];
        }


        @SuppressWarnings("unchecked")
        private void handleMessage(Connstruct _c, Message _m) throws DBusException {

            LOGGER.debug("enter");

            LOGGER.trace("Handling message {}  from {}", _m, _c.unique);

            if (!(_m instanceof MethodCall)) {
                return;
            }
            Object[] args = _m.getParameters();

            Class<? extends Object>[] cs = new Class[args.length];

            for (int i = 0; i < cs.length; i++) {
                cs[i] = args[i].getClass();
            }

            java.lang.reflect.Method meth = null;
            Object rv = null;

            try {
                meth = DBusServer.class.getMethod(_m.getName(), cs);
                try {
                    this.c = _c;
                    this.m = _m;
                    rv = meth.invoke(dbusServer, args);
                    if (null == rv) {
                        send(_c, new MethodReturn("org.freedesktop.DBus", (MethodCall) _m, null), true);
                    } else {
                        String sig = Marshalling.getDBusType(meth.getGenericReturnType())[0];
                        send(_c, new MethodReturn("org.freedesktop.DBus", (MethodCall) _m, sig, rv), true);
                    }
                } catch (InvocationTargetException ite) {
                    LOGGER.debug("", ite);
                    send(_c, new org.freedesktop.dbus.errors.Error("org.freedesktop.DBus", _m, ite.getCause()));
                } catch (DBusExecutionException dbee) {
                   LOGGER.debug("", dbee);
                    send(_c, new org.freedesktop.dbus.errors.Error("org.freedesktop.DBus", _m, dbee));
                } catch (Exception e) {
                    LOGGER.debug("", e);
                    send(_c, new org.freedesktop.dbus.errors.Error("org.freedesktop.DBus", _c.unique, "org.freedesktop.DBus.Error.GeneralError", _m.getSerial(), "s", "An error occurred while calling " + _m.getName()));
                }
            } catch (NoSuchMethodException exNsm) {
                send(_c, new org.freedesktop.dbus.errors.Error("org.freedesktop.DBus", _c.unique, "org.freedesktop.DBus.Error.UnknownMethod", _m.getSerial(), "s", "This service does not support " + _m.getName()));
            }

            LOGGER.debug("exit");

        }

        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public String Introspect() {
            return "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n" + "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n" + "<node>\n" + "  <interface name=\"org.freedesktop.DBus.Introspectable\">\n" + "    <method name=\"Introspect\">\n"
                    + "      <arg name=\"data\" direction=\"out\" type=\"s\"/>\n" + "    </method>\n" + "  </interface>\n" + "  <interface name=\"org.freedesktop.DBus\">\n" + "    <method name=\"RequestName\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"in\" type=\"u\"/>\n"
                    + "      <arg direction=\"out\" type=\"u\"/>\n" + "    </method>\n" + "    <method name=\"ReleaseName\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"out\" type=\"u\"/>\n" + "    </method>\n" + "    <method name=\"StartServiceByName\">\n"
                    + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"in\" type=\"u\"/>\n" + "      <arg direction=\"out\" type=\"u\"/>\n" + "    </method>\n" + "    <method name=\"Hello\">\n" + "      <arg direction=\"out\" type=\"s\"/>\n" + "    </method>\n"
                    + "    <method name=\"NameHasOwner\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"out\" type=\"b\"/>\n" + "    </method>\n" + "    <method name=\"ListNames\">\n" + "      <arg direction=\"out\" type=\"as\"/>\n" + "    </method>\n"
                    + "    <method name=\"ListActivatableNames\">\n" + "      <arg direction=\"out\" type=\"as\"/>\n" + "    </method>\n" + "    <method name=\"AddMatch\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "    </method>\n" + "    <method name=\"RemoveMatch\">\n"
                    + "      <arg direction=\"in\" type=\"s\"/>\n" + "    </method>\n" + "    <method name=\"GetNameOwner\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"out\" type=\"s\"/>\n" + "    </method>\n" + "    <method name=\"ListQueuedOwners\">\n"
                    + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"out\" type=\"as\"/>\n" + "    </method>\n" + "    <method name=\"GetConnectionUnixUser\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"out\" type=\"u\"/>\n" + "    </method>\n"
                    + "    <method name=\"GetConnectionUnixProcessID\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n" + "      <arg direction=\"out\" type=\"u\"/>\n" + "    </method>\n" + "    <method name=\"GetConnectionSELinuxSecurityContext\">\n" + "      <arg direction=\"in\" type=\"s\"/>\n"
                    + "      <arg direction=\"out\" type=\"ay\"/>\n" + "    </method>\n" + "    <method name=\"ReloadConfig\">\n" + "    </method>\n" + "    <signal name=\"NameOwnerChanged\">\n" + "      <arg type=\"s\"/>\n" + "      <arg type=\"s\"/>\n" + "      <arg type=\"s\"/>\n" + "    </signal>\n"
                    + "    <signal name=\"NameLost\">\n" + "      <arg type=\"s\"/>\n" + "    </signal>\n" + "    <signal name=\"NameAcquired\">\n" + "      <arg type=\"s\"/>\n" + "    </signal>\n" + "  </interface>\n" + "</node>";
        }

        @Override
        public void Ping() {
        }

        @Override
        public void run() {

            LOGGER.debug("enter");

            while (isRunning()) {
                Message msg;
                List<WeakReference<Connstruct>> wcs;
                // block on outqueue
                synchronized (localqueue) {
                    while (localqueue.size() == 0) {
                        try {
                            localqueue.wait();
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                    msg = localqueue.head();
                    wcs = localqueue.remove(msg);
                }
                if (null != wcs) {
                    try {
                        for (WeakReference<Connstruct> wc : wcs) {
                            Connstruct constructor = wc.get();
                            if (null != constructor) {

                                LOGGER.trace("<localqueue> Got message {} from {}", msg, constructor);

                                handleMessage(constructor, msg);
                            }
                        }
                    } catch (DBusException dbe) {
                        LOGGER.debug("", dbe);
                    }
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.info("Discarding {} connection reaped", msg);
                }
            }

            LOGGER.debug("exit");

        }

        @Override
        public String[] ListActivatableNames() {
            return null;
        }

        @Override
        public Map<String, Variant<?>> GetConnectionCredentials(String _busName) {
            return null;
        }

        @Override
        public Byte[] GetAdtAuditSessionData(String _busName) {
            return null;
        }

        @Override
        public void UpdateActivationEnvironment(Map<String, String>[] _environment) {

        }

        @Override
        public String GetId() {
            return null;
        }

        @Override
        public String GetMachineId() {
            return machineId;
        }

    }

    public class Sender extends Thread {
        private final Logger logger = LoggerFactory.getLogger(getClass());

        public Sender() {
            setName("Sender");
        }

        @Override
        public void run() {

            logger.debug("enter");

            while (isRunning()) {

                logger.trace("Acquiring lock on outqueue and blocking for data");

                Message m = null;
                List<WeakReference<Connstruct>> wcs = null;
                // block on outqueue
                synchronized (outqueue) {
                    while (outqueue.size() == 0) {
                        try {
                            outqueue.wait();
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }

                    m = outqueue.head();
                    wcs = outqueue.remove(m);
                }
                if (null != wcs) {
                    for (WeakReference<Connstruct> wc : wcs) {
                        Connstruct c = wc.get();
                        if (null != c) {

                            logger.trace("<outqueue> Got message {} for {}", m, c.unique);
                            logger.info("Sending message {} to {}", m, c.unique);

                            try {
                                c.mout.writeMessage(m);
                            } catch (IOException ioe) {
                                logger.debug("", ioe);
                                removeConnection(c);
                            }
                        }
                    }
                } else {
                    logger.info("Discarding {} connection reaped", m);
                }
            }

            logger.debug("exit");

        }
    }

    public class Reader extends Thread {
        private Connstruct                conn;
        private WeakReference<Connstruct> weakconn;
        private boolean                   lrun = true;

        public Reader(Connstruct _conn) {
            this.conn = _conn;
            weakconn = new WeakReference<>(_conn);
            setName("Reader");
        }

        public void stopRunning() {
            lrun = false;
        }

        @Override
        public void run() {

            LOGGER.debug("enter");

            while (isRunning() && lrun) {

                Message m = null;
                try {
                    m = conn.min.readMessage();
                } catch (IOException ioe) {
                    LOGGER.debug("", ioe);
                    removeConnection(conn);
                } catch (DBusException dbe) {
                    LOGGER.debug("", dbe);
                    if (dbe instanceof FatalException) {
                        removeConnection(conn);
                    }
                }

                if (null != m) {
                    LOGGER.info("Read {} from {}", m, conn.unique);

                    synchronized (inqueue) {
                        inqueue.putLast(m, weakconn);
                        inqueue.notifyAll();
                    }
                }
            }
            conn = null;

            LOGGER.debug("exit");

        }
    }

    private Map<Connstruct, Reader>                      conns       = new HashMap<>();
    private HashMap<String, Connstruct>                  names       = new HashMap<>();
    private MagicMap<Message, WeakReference<Connstruct>> outqueue    = new MagicMap<>("out");
    private MagicMap<Message, WeakReference<Connstruct>> inqueue     = new MagicMap<>("in");
    private MagicMap<Message, WeakReference<Connstruct>> localqueue  = new MagicMap<>("local");
    private List<Connstruct>                             sigrecips   = new ArrayList<>();
    private final AtomicBoolean                          run        = new AtomicBoolean(true);
    private int                                          nextUnique = 0;
    private Object                                       uniqueLock = new Object();
    //CHECKSTYLE:OFF
    DBusServer                                           dbusServer = new DBusServer();
    Sender                                               sender      = new Sender();
    //CHECKSTYLE:ON

    public DBusDaemon() {
        setName("Daemon");
        synchronized (names) {
            names.put("org.freedesktop.DBus", null);
        }
    }

    private void send(Connstruct c, Message m) {
        send(c, m, false);
    }

    private void send(Connstruct c, Message m, boolean head) {

        LOGGER.debug("enter");
        if (null == c) {
            LOGGER.trace("Queing message {} for all connections", m);
        } else {
            LOGGER.trace("Queing message {} for {}", m, c.unique);
        }

        // send to all connections
        if (null == c) {
            synchronized (conns) {
                synchronized (outqueue) {
                    for (Connstruct d : conns.keySet()) {
                        if (head) {
                            outqueue.putFirst(m, new WeakReference<>(d));
                        } else {
                            outqueue.putLast(m, new WeakReference<>(d));
                        }
                    }
                    outqueue.notifyAll();
                }
            }
        } else {
            synchronized (outqueue) {
                if (head) {
                    outqueue.putFirst(m, new WeakReference<>(c));
                } else {
                    outqueue.putLast(m, new WeakReference<>(c));
                }
                outqueue.notifyAll();
            }
        }

        LOGGER.debug("exit");

    }

    private List<Connstruct> findSignalMatches(DBusSignal sig) {

        LOGGER.debug("enter");

        List<Connstruct> l;
        synchronized (sigrecips) {
            l = new ArrayList<>(sigrecips);
        }

        LOGGER.debug("exit");

        return l;
    }

    @Override
    public void run() {

        LOGGER.debug("enter");

        while (isRunning()) {
            try {
                Message m;
                List<WeakReference<Connstruct>> wcs;
                synchronized (inqueue) {
                    while (0 == inqueue.size()) {
                        try {
                            inqueue.wait();
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }

                    m = inqueue.head();
                    wcs = inqueue.remove(m);
                }
                if (null != wcs) {
                    for (WeakReference<Connstruct> wc : wcs) {
                        Connstruct c = wc.get();
                        if (null != c) {
                            LOGGER.info("<inqueue> Got message {} from {}", m, c.unique);
                            // check if they have hello'd
                            if (null == c.unique && (!(m instanceof MethodCall) || !"org.freedesktop.DBus".equals(m.getDestination()) || !"Hello".equals(m.getName()))) {
                                send(c, new Error("org.freedesktop.DBus", null, "org.freedesktop.DBus.Error.AccessDenied", m.getSerial(), "s", "You must send a Hello message"));
                            } else {
                                try {
                                    if (null != c.unique) {
                                        m.setSource(c.unique);
                                    }
                                } catch (DBusException dbe) {
                                    LOGGER.debug("", dbe);
                                    send(c, new Error("org.freedesktop.DBus", null, "org.freedesktop.DBus.Error.GeneralError", m.getSerial(), "s", "Sending message failed"));
                                }

                                if ("org.freedesktop.DBus".equals(m.getDestination())) {
                                    synchronized (localqueue) {
                                        localqueue.putLast(m, wc);
                                        localqueue.notifyAll();
                                    }
                                } else {
                                    if (m instanceof DBusSignal) {
                                        List<Connstruct> list = findSignalMatches((DBusSignal) m);
                                        for (Connstruct d : list) {
                                            send(d, m);
                                        }
                                    } else {
                                        Connstruct dest = names.get(m.getDestination());

                                        if (null == dest) {
                                            send(c, new Error("org.freedesktop.DBus", null, "org.freedesktop.DBus.Error.ServiceUnknown", m.getSerial(), "s", String.format("The name `%s' does not exist", m.getDestination())));
                                        } else {
                                            send(dest, m);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (DBusException dbe) {
                LOGGER.debug("", dbe);
            }
        }

        LOGGER.debug("exit");

    }

    private void removeConnection(Connstruct c) {

        LOGGER.debug("enter");

        boolean exists = false;
        synchronized (conns) {
            if (conns.containsKey(c)) {
                Reader r = conns.get(c);
                exists = true;
                r.stopRunning();
                conns.remove(c);
            }
        }
        if (exists) {
            try {
                if (null != c.usock) {
                    c.usock.close();
                }
                if (null != c.tsock) {
                    c.tsock.close();
                }
            } catch (IOException exIo) {
            }
            synchronized (names) {
                List<String> toRemove = new ArrayList<>();
                for (String name : names.keySet()) {
                    if (names.get(name) == c) {
                        toRemove.add(name);
                        try {
                            send(null, new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameOwnerChanged", "sss", name, c.unique, ""));
                        } catch (DBusException dbe) {
                            LOGGER.debug("", dbe);
                        }
                    }
                }
                for (String name : toRemove) {
                    names.remove(name);
                }
            }
        }

        LOGGER.debug("exit");

    }

    public void addSock(Socket s) throws IOException {

        LOGGER.debug("enter");

        LOGGER.debug("New Client");

        Connstruct c = new Connstruct(s);
        Reader r = new Reader(c);
        synchronized (conns) {
            conns.put(c, r);
        }
        r.start();

        LOGGER.debug("exit");

    }

    @Override
    public void close() {
        run.set(false);
        interrupt();
    }

    public boolean isRunning() {
        return this.run.get() && isAlive();
    }

    public static void syntax() {
        System.out.println("Syntax: DBusDaemon [--version] [-v] [--help] [-h] [--listen address] [-l address] [--print-address] [-r] [--pidfile file] [-p file] [--addressfile file] [-a file] [--unix] [-u] [--tcp] [-t] ");
        System.exit(1);
    }

    public static void version() {
        System.out.println("D-Bus Java Version: " + System.getProperty("Version"));
        System.exit(1);
    }

    public static void saveFile(String data, String file) throws IOException {
        PrintWriter w = new PrintWriter(new FileOutputStream(file));
        w.println(data);
        w.close();
    }

    public static void main(String[] args) throws Exception {
        LOGGER.debug("enter");
        String addr = null;
        String pidfile = null;
        String addrfile = null;
        boolean printaddress = false;
        boolean unix = true;
        boolean tcp = false;

        // parse options
        try {
            for (int i = 0; i < args.length; i++) {
                if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                    syntax();
                } else if ("--version".equals(args[i]) || "-v".equals(args[i])) {
                    version();
                } else if ("--listen".equals(args[i]) || "-l".equals(args[i])) {
                    addr = args[++i];
                } else if ("--pidfile".equals(args[i]) || "-p".equals(args[i])) {
                    pidfile = args[++i];
                } else if ("--addressfile".equals(args[i]) || "-a".equals(args[i])) {
                    addrfile = args[++i];
                } else if ("--print-address".equals(args[i]) || "-r".equals(args[i])) {
                    printaddress = true;
                } else if ("--unix".equals(args[i]) || "-u".equals(args[i])) {
                    unix = true;
                    tcp = false;
                } else if ("--tcp".equals(args[i]) || "-t".equals(args[i])) {
                    tcp = true;
                    unix = false;
                } else {
                    syntax();
                }
            }
        } catch (ArrayIndexOutOfBoundsException exAioob) {
            syntax();
        }

        // generate a random address if none specified
        if (null == addr && unix) {
            addr = DirectConnection.createDynamicSession();
        } else if (null == addr && tcp) {
            addr = DirectConnection.createDynamicTCPSession();
        }

        BusAddress address = new BusAddress(addr);
        if (!address.hasGuid()) {
            addr += ",guid=" + TransportFactory.genGUID();
            address = new BusAddress(addr);
        }

        // print address to stdout
        if (printaddress) {
            System.out.println(addr);
        }

        // print address to file
        if (null != addrfile) {
            saveFile(addr, addrfile);
        }

        // print PID to file
        if (null != pidfile) {
            saveFile(System.getProperty("Pid"), pidfile);
        }

        // start the daemon
        LOGGER.info("Binding to {}", addr);
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon()) {
	        daemon.setAddress(address);
	        daemon.startInForeground();
        }
        LOGGER.debug("exit");
    }
}
