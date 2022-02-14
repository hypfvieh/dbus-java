package org.freedesktop.dbus.bin;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.errors.AccessDenied;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.errors.MatchRuleInvalid;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBus.NameOwnerChanged;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.messages.MethodReturn;
import org.freedesktop.dbus.spi.message.InputStreamMessageReader;
import org.freedesktop.dbus.spi.message.OutputStreamMessageWriter;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A replacement DBusDaemon
 */
public class DBusDaemon extends Thread implements Closeable {
    public static final int                                          QUEUE_POLL_WAIT = 500;

    private static final Logger                                      LOGGER          =
            LoggerFactory.getLogger(DBusDaemon.class);

    private final Map<ConnectionStruct, DBusDaemonReaderThread>      conns           = new ConcurrentHashMap<>();
    private final Map<String, ConnectionStruct>                      names           = Collections.synchronizedMap(new HashMap<>());
    private final MagicMap<Message, WeakReference<ConnectionStruct>> outqueue        = new MagicMap<>("out");
    private final MagicMap<Message, WeakReference<ConnectionStruct>> inqueue         = new MagicMap<>("in");
    private final MagicMap<Message, WeakReference<ConnectionStruct>> localqueue      = new MagicMap<>("local");
    private final List<ConnectionStruct>                             sigrecips       = new ArrayList<>();
    private final Object                                             uniqueLock      = new Object();
    private final DBusServer                                         dbusServer      = new DBusServer();
    private final DBusDaemonSenderThread                             sender          = new DBusDaemonSenderThread();
    private final AtomicBoolean                                      run             = new AtomicBoolean(false);
    private int                                                      nextUnique      = 0;

    public DBusDaemon() {
        setName(getClass().getSimpleName() + "-Thread");
        names.put("org.freedesktop.DBus", null);
    }

    private void send(ConnectionStruct _connStruct, Message _msg) {
        send(_connStruct, _msg, false);
    }

    private void send(ConnectionStruct _connStruct, Message _msg, boolean _head) {

        // send to all connections
        if (null == _connStruct) {
            LOGGER.trace("Queuing message {} for all connections", _msg);
            synchronized (conns) {
                synchronized (outqueue) {
                    for (ConnectionStruct d : conns.keySet()) {
                        if (_head) {
                            outqueue.putFirst(_msg, new WeakReference<>(d));
                        } else {
                            outqueue.putLast(_msg, new WeakReference<>(d));
                        }
                    }
                    outqueue.notifyAll();
                }
            }
        } else {
            LOGGER.trace("Queuing message {} for {}", _msg, _connStruct.unique);
            synchronized (outqueue) {
                if (_head) {
                    outqueue.putFirst(_msg, new WeakReference<>(_connStruct));
                } else {
                    outqueue.putLast(_msg, new WeakReference<>(_connStruct));
                }
                outqueue.notifyAll();
            }
        }

    }

    // TODO: Why the hell is a signal given when it is never used?
    private List<ConnectionStruct> findSignalMatches(DBusSignal _sig) {
        List<ConnectionStruct> l;
        synchronized (sigrecips) {
            l = new ArrayList<>(sigrecips);
        }
        return l;
    }

    @Override
    public synchronized void start() {
        super.start();

        dbusServer.start();
        sender.start();
    }

    @Override
    public void run() {
        run.set(true);

        while (isRunning()) {
            try {
                Message m;
                List<WeakReference<ConnectionStruct>> wcs;
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
                    for (WeakReference<ConnectionStruct> wc : wcs) {
                        ConnectionStruct c = wc.get();
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
                                        List<ConnectionStruct> list = findSignalMatches((DBusSignal) m);
                                        for (ConnectionStruct d : list) {
                                            send(d, m);
                                        }
                                    } else {
                                        ConnectionStruct dest = names.get(m.getDestination());

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

    }

    public boolean isRunning() {
        return run.get() && isAlive();
    }

    @Override
    public void close() {
        run.set(false);
        sender.terminate();
        dbusServer.terminate();
        interrupt();
    }

    private void removeConnection(ConnectionStruct _c) {

        boolean exists = false;
        synchronized (conns) {
            if (conns.containsKey(_c)) {
                DBusDaemonReaderThread r = conns.get(_c);
                exists = true;
                r.stopRunning();
                conns.remove(_c);
            }
        }
        if (exists) {
            try {
                if (null != _c.socketChannel) {
                    _c.socketChannel.close();
                }
            } catch (IOException _exIo) {
            }

            synchronized (names) {
                List<String> toRemove = new ArrayList<>();
                for (String name : names.keySet()) {
                    if (names.get(name) == _c) {
                        toRemove.add(name);
                        try {
                            send(null, new NameOwnerChanged("/org/freedesktop/DBus", name, _c.unique, ""));
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

    }

    void addSock(SocketChannel _sock) throws IOException {
        LOGGER.debug("New Client");

        ConnectionStruct c = new ConnectionStruct(_sock);
        DBusDaemonReaderThread r = new DBusDaemonReaderThread(c);
        conns.put(c, r);
        r.start();

    }

    public static void syntax() {
        System.out.println("Syntax: DBusDaemon [--version] [-v] [--help] [-h] [--listen address] [-l address] [--print-address] [-r] [--pidfile file] [-p file] [--addressfile file] [-a file] [--unix] [-u] [--tcp] [-t] ");
        System.exit(1);
    }

    public static void version() {
        System.out.println("D-Bus Java Version: " + System.getProperty("Version"));
        System.exit(1);
    }

    public static void saveFile(String _data, String _file) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileOutputStream(_file))) {
            w.println(_data);
        }
    }

    public static void main(String[] _args) throws Exception {

        String addr = null;
        String pidfile = null;
        String addrfile = null;
        boolean printaddress = false;
        boolean unix = true;
        boolean tcp = false;

        // parse options
        try {
            for (int i = 0; i < _args.length; i++) {
                if ("--help".equals(_args[i]) || "-h".equals(_args[i])) {
                    syntax();
                } else if ("--version".equals(_args[i]) || "-v".equals(_args[i])) {
                    version();
                } else if ("--listen".equals(_args[i]) || "-l".equals(_args[i])) {
                    addr = _args[++i];
                } else if ("--pidfile".equals(_args[i]) || "-p".equals(_args[i])) {
                    pidfile = _args[++i];
                } else if ("--addressfile".equals(_args[i]) || "-a".equals(_args[i])) {
                    addrfile = _args[++i];
                } else if ("--print-address".equals(_args[i]) || "-r".equals(_args[i])) {
                    printaddress = true;
                } else if ("--unix".equals(_args[i]) || "-u".equals(_args[i])) {
                    unix = true;
                    tcp = false;
                } else if ("--tcp".equals(_args[i]) || "-t".equals(_args[i])) {
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
            addr = TransportBuilder.createDynamicSession("UNIX", true);
        } else if (null == addr && tcp) {
            addr = TransportBuilder.createDynamicSession("TCP", true);
        }

        BusAddress address = new BusAddress(addr);

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
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(address)) {
            daemon.startInForeground();
        }

    }

    /**
     * Create a 'NameAcquired' signal manually.
     * This is required because the implementation in DBusNameAquired is for receiving of this signal only.
     * 
     * @param _name name to announce
     * 
     * @return signal
     * @throws DBusException if signal creation fails
     */
    private DBusSignal generateNameAcquiredSignal(String _name) throws DBusException {
        return new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameAcquired", "s", _name);
    }

    /**
     * Create a 'NameOwnerChanged' signal manually.
     * This is required because the implementation in DBusNameAquired is for receiving of this signal only.
     * 
     * @param _name name to announce
     * @param _oldOwner previous owner
     * @param _newOwner new owner
     * 
     * @return signal
     * @throws DBusException if signal creation fails
     */
    private DBusSignal generatedNameOwnerChangedSignal(String _name, String _oldOwner, String _newOwner) throws DBusException {
        return new DBusSignal("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameOwnerChanged", "sss", _name, _oldOwner, _newOwner);
    }

    
    public static class ConnectionStruct {
        private final InputStreamMessageReader  inputReader;
        private final OutputStreamMessageWriter outputWriter;
        private final SocketChannel             socketChannel;
        private String                          unique;

        ConnectionStruct(SocketChannel _sock) throws IOException {
            socketChannel = _sock;
            inputReader = new InputStreamMessageReader(socketChannel);
            outputWriter = new OutputStreamMessageWriter(socketChannel);
        }

        @Override
        public String toString() {
            return null == unique ? ":?-?" : unique;
        }
    }

    public class DBusServer extends Thread implements DBus, Introspectable, Peer {

        private final String machineId;
        private ConnectionStruct connStruct;
        private volatile AtomicBoolean running = new AtomicBoolean(true);

        public DBusServer() {
            setName("DBusServer");
            String ascii;
            try {
                ascii = Hexdump.toAscii(MessageDigest.getInstance("MD5").digest(InetAddress.getLocalHost().getHostName().getBytes()));
            } catch (NoSuchAlgorithmException | UnknownHostException _ex) {
                ascii = this.hashCode() + "";
            }

            machineId = ascii;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String Hello() {
            synchronized (connStruct) {
                if (null != connStruct.unique) {
                    throw new AccessDenied("Connection has already sent a Hello message");
                }
                synchronized (uniqueLock) {
                    connStruct.unique = ":1." + (++nextUnique);
                }
            }
            names.put(connStruct.unique, connStruct);

            LOGGER.info("Client {} registered", connStruct.unique);

            try {
                send(connStruct, generateNameAcquiredSignal(connStruct.unique));
                send(null, generatedNameOwnerChangedSignal(connStruct.unique, "", connStruct.unique));
            } catch (DBusException dbe) {
                LOGGER.debug("", dbe);
            }

            return connStruct.unique;
        }


        @Override
        public String[] ListNames() {
            String[] ns;
            Set<String> nss = names.keySet();
            ns = nss.toArray(new String[0]);
            return ns;
        }

        @Override
        public boolean NameHasOwner(String _name) {
            return names.containsKey(_name);
        }

        @Override
        public String GetNameOwner(String _name) {

            ConnectionStruct owner = names.get(_name);
            String o;
            if (null == owner) {
                o = "";
            } else {
                o = owner.unique;
            }

            return o;
        }

        @Override
        public UInt32 GetConnectionUnixUser(String _connectionName) {
            return new UInt32(0);
        }

        @Override
        public UInt32 StartServiceByName(String _name, UInt32 _flags) {
            return new UInt32(0);
        }

        @Override
        public UInt32 RequestName(String _name, UInt32 _flags) {
            boolean exists = false;
            synchronized (names) {
                if (!(exists = names.containsKey(_name))) {
                    names.put(_name, connStruct);
                }
            }

            int rv;
            if (exists) {
                rv = DBus.DBUS_REQUEST_NAME_REPLY_EXISTS;
            } else {

                LOGGER.info("Client {} acquired name {}", connStruct.unique, _name);

                rv = DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER;
                try {
                    send(connStruct, generateNameAcquiredSignal(_name));
                    send(null, generatedNameOwnerChangedSignal(_name, "", connStruct.unique));
                } catch (DBusException dbe) {
                    LOGGER.debug("", dbe);
                }
            }
            return new UInt32(rv);
        }


        @Override
        public UInt32 ReleaseName(String _name) {

            boolean exists = false;
            synchronized (names) {
                if (names.containsKey(_name) && names.get(_name).equals(connStruct)) {
                    exists = names.remove(_name) != null;
                }
            }

            int rv;
            if (!exists) {
                rv = DBus.DBUS_RELEASE_NAME_REPLY_NON_EXISTANT;
            } else {
                LOGGER.info("Client {} acquired name {}", connStruct.unique, _name);
                rv = DBus.DBUS_RELEASE_NAME_REPLY_RELEASED;
                try {
                    send(connStruct, new NameLost("/org/freedesktop/DBus", _name));
                    send(null, new NameOwnerChanged("/org/freedesktop/DBus", _name, connStruct.unique, ""));
                } catch (DBusException dbe) {
                    LOGGER.debug("", dbe);
                }
            }

            return new UInt32(rv);
        }

        @Override
        public void AddMatch(String _matchrule) throws MatchRuleInvalid {

            LOGGER.trace("Adding match rule: {}", _matchrule);

            synchronized (sigrecips) {
                if (!sigrecips.contains(connStruct)) {
                    sigrecips.add(connStruct);
                }
            }
        }

        @Override
        public void RemoveMatch(String _matchrule) throws MatchRuleInvalid {
            LOGGER.trace("Removing match rule: {}", _matchrule);
        }

        @Override
        public String[] ListQueuedOwners(String _name) {
            return new String[0];
        }

        @Override
        public UInt32 GetConnectionUnixProcessID(String _connectionName) {
            return new UInt32(0);
        }

        @Override
        public Byte[] GetConnectionSELinuxSecurityContext(String _args) {
            return new Byte[0];
        }


        @SuppressWarnings("unchecked")
        private void handleMessage(ConnectionStruct _connStruct, Message _msg) throws DBusException {
            LOGGER.trace("Handling message {}  from {}", _msg, _connStruct.unique);

            if (!(_msg instanceof MethodCall)) {
                return;
            }
            Object[] args = _msg.getParameters();

            Class<? extends Object>[] cs = new Class[args.length];

            for (int i = 0; i < cs.length; i++) {
                cs[i] = args[i].getClass();
            }

            java.lang.reflect.Method meth = null;
            Object rv = null;

            try {
                meth = DBusServer.class.getMethod(_msg.getName(), cs);
                try {
                    this.connStruct = _connStruct;
                    rv = meth.invoke(dbusServer, args);
                    if (null == rv) {
                        
                        send(_connStruct, new MethodReturn("org.freedesktop.DBus", (MethodCall) _msg, null), true);
                    } else {
                        String sig = Marshalling.getDBusType(meth.getGenericReturnType())[0];
                        send(_connStruct, new MethodReturn("org.freedesktop.DBus", (MethodCall) _msg, sig, rv), true);
                    }
                } catch (InvocationTargetException _exIte) {
                    LOGGER.debug("", _exIte);
                    send(_connStruct, new Error("org.freedesktop.DBus", _msg, _exIte.getCause()));
                } catch (DBusExecutionException _exDnEe) {
                   LOGGER.debug("", _exDnEe);
                   send(_connStruct, new Error("org.freedesktop.DBus", _msg, _exDnEe));
                } catch (Exception _ex) {
                    LOGGER.debug("", _ex);
                    send(_connStruct, new Error("org.freedesktop.DBus", _connStruct.unique, "org.freedesktop.DBus.Error.GeneralError", _msg.getSerial(), "s", "An error occurred while calling " + _msg.getName()));
                }
            } catch (NoSuchMethodException _exNsm) {
                send(_connStruct, new Error("org.freedesktop.DBus", _connStruct.unique, "org.freedesktop.DBus.Error.UnknownMethod", _msg.getSerial(), "s", "This service does not support " + _msg.getName()));
            }

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

            while (isRunning() && running.get()) {
                Message msg;
                List<WeakReference<ConnectionStruct>> wcs;
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
                        for (WeakReference<ConnectionStruct> wc : wcs) {
                            ConnectionStruct constructor = wc.get();
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

        }

        public void terminate() {
            running.set(false);
            interrupt();
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

    public class DBusDaemonSenderThread extends Thread {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private volatile AtomicBoolean running = new AtomicBoolean(true);


        public DBusDaemonSenderThread() {
            setName(getClass().getSimpleName());
        }

        @Override
        public void run() {
            while (isRunning() && running.get()) {

                logger.trace("Acquiring lock on outqueue and blocking for data");

                Message m = null;
                List<WeakReference<ConnectionStruct>> wcs = null;
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
                    for (WeakReference<ConnectionStruct> wc : wcs) {
                        ConnectionStruct c = wc.get();
                        if (null != c) {

                            logger.trace("<outqueue> Got message {} for {}", m, c.unique);
                            logger.info("Sending message {} to {}", m, c.unique);

                            try {
                                c.outputWriter.writeMessage(m);
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
        }

        public void terminate() {
            running.set(false);
            interrupt();
        }
    }

    public class DBusDaemonReaderThread extends Thread {
        private ConnectionStruct                      conn;
        private final WeakReference<ConnectionStruct> weakconn;
        private volatile boolean                      lrun = true;

        public DBusDaemonReaderThread(ConnectionStruct _conn) {
            this.conn = _conn;
            weakconn = new WeakReference<>(_conn);
            setName(getClass().getSimpleName());
        }

        public void stopRunning() {
            lrun = false;
        }

        @Override
        public void run() {

            while (isRunning() && lrun) {

                Message m = null;
                try {
                    m = conn.inputReader.readMessage();
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

        }
    }


    static class MagicMap<A, B> {
        private final Logger                logger = LoggerFactory.getLogger(getClass());

        private final Map<A, LinkedList<B>> m;
        private final LinkedList<A>         q;
        private final String                name;

        MagicMap(String _name) {
            m = new HashMap<>();
            q = new LinkedList<>();
            this.name = _name;
        }

        public A head() {
            return q.getFirst();
        }

        public void putFirst(A _a, B _b) {
            logger.debug("<{}> Queueing {{} => {}}", name, _a, _b);

            if (m.containsKey(_a)) {
                m.get(_a).add(_b);
            } else {
                LinkedList<B> l = new LinkedList<>();
                l.add(_b);
                m.put(_a, l);
            }
            q.addFirst(_a);
        }

        public void putLast(A _a, B _b) {
            logger.debug("<{}> Queueing {{} => {}}", name, _a, _b);

            if (m.containsKey(_a)) {
                m.get(_a).add(_b);
            } else {
                LinkedList<B> l = new LinkedList<>();
                l.add(_b);
                m.put(_a, l);
            }
            q.addLast(_a);
        }

        public List<B> remove(A _a) {
            logger.debug("<{}> Removing {{}}", name, _a);

            q.remove(_a);
            return m.remove(_a);
        }

        public int size() {
            return q.size();
        }
    }
}
