package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder.SaslAuthMode;
import org.freedesktop.dbus.connections.transports.TransportConnection;
import org.freedesktop.dbus.errors.AccessDenied;
import org.freedesktop.dbus.errors.MatchRuleInvalid;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBus.NameOwnerChanged;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.interfaces.Peer;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.matchrules.MatchRuleParser;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.AddressBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A replacement DBusDaemon
 */
public class DBusDaemon extends Thread implements Closeable {

    public static final int                                                     QUEUE_POLL_WAIT = 500;

    private static final String                                                 DBUS_BUSPATH    = "/org/freedesktop/DBus";
    private static final String                                                 DBUS_BUSNAME    = "org.freedesktop.DBus";
    private static final String[]                                               EMPTY_STRING_ARRAY = new String[0];

    private static final Logger                                                 LOGGER          =
            LoggerFactory.getLogger(DBusDaemon.class);

    private final Map<ConnectionStruct, DBusDaemonReaderThread>                 conns           =
            new ConcurrentHashMap<>();
    private final Map<String, ConnectionStruct>                                 names           =
            Collections.synchronizedMap(new HashMap<>()); // required because of "null" key

    private final BlockingDeque<Pair<Message, WeakReference<ConnectionStruct>>> outqueue       =
            new LinkedBlockingDeque<>();
    private final BlockingDeque<Pair<Message, WeakReference<ConnectionStruct>>> inqueue        =
            new LinkedBlockingDeque<>();

    private final List<ConnectionStruct>                                        sigrecips       = new ArrayList<>();
    private final DBusServer                                                    dbusServer      = new DBusServer();

    private final DBusDaemonSenderThread                                        sender          =
            new DBusDaemonSenderThread();
    private final AtomicBoolean                                                 run             =
            new AtomicBoolean(false);
    private final AtomicInteger                                                 nextUnique      = new AtomicInteger(0);

    private final AbstractTransport                                             transport;

    public DBusDaemon(AbstractTransport _transport) {
        setName(getClass().getSimpleName() + "-Thread");
        transport = _transport;
        names.put(DBUS_BUSNAME, null);
    }

    private void send(ConnectionStruct _connStruct, Message _msg) {
        send(_connStruct, _msg, false);
    }

    private void send(ConnectionStruct _connStruct, Message _msg, boolean _head) {

        // send to all connections
        if (_connStruct == null) {
            LOGGER.trace("Queuing message {} for all connections", _msg);
            for (ConnectionStruct d : conns.keySet()) {
                if (d.connection == null || d.connection.getChannel() == null || !d.connection.getChannel().isConnected()) {
                    LOGGER.debug("Ignoring broadcast message for disconnected connection {}: {}", d.connection, _msg);
                } else {
                    if (_head) {
                        outqueue.addFirst(new Pair<>(_msg, new WeakReference<>(d)));
                    } else {
                        outqueue.addLast(new Pair<>(_msg, new WeakReference<>(d)));
                    }
                }
            }
        } else {
            LOGGER.trace("Queuing message {} for {}", _msg, _connStruct.unique);
            if (_head) {
                outqueue.addFirst(new Pair<>(_msg, new WeakReference<>(_connStruct)));
            } else {
                outqueue.addLast(new Pair<>(_msg, new WeakReference<>(_connStruct)));
            }
        }
    }

    @Override
    public void run() {
        run.set(true);
        sender.start();

        while (isRunning()) {
            try {
                Pair<Message, WeakReference<ConnectionStruct>> pollFirst = inqueue.take();
                ConnectionStruct connectionStruct = pollFirst.second.get();
                if (connectionStruct != null) {
                    Message m = pollFirst.first;
                    logMessage("<inqueue> Got message {} from {}", m, connectionStruct.unique);
                    MessageFactory messageFactory = connectionStruct.connection.getMessageFactory();
                    // check if they have hello'd
                    if (null == connectionStruct.unique && (!(m instanceof MethodCall) || !DBUS_BUSNAME.equals(m.getDestination()) || !"Hello".equals(m.getName()))) {
                        send(connectionStruct,
                            messageFactory.createError(DBUS_BUSNAME, null, "org.freedesktop.DBus.Error.AccessDenied", m.getSerial(), "s", "You must send a Hello message"));
                    } else {
                        try {
                            if (null != connectionStruct.unique) {
                                m.setSource(connectionStruct.unique);
                                LOGGER.trace("Updated source to {}", connectionStruct.unique);
                            }
                        } catch (DBusException _ex) {
                            LOGGER.debug("Error setting source", _ex);
                            send(connectionStruct, messageFactory.createError(DBUS_BUSNAME, null, "org.freedesktop.DBus.Error.GeneralError", m.getSerial(), "s", "Sending message failed"));
                        }

                        if (DBUS_BUSNAME.equals(m.getDestination())) {
                            dbusServer.handleMessage(connectionStruct, pollFirst.first);
                        } else {
                            handleMatchRules(m);

                            if (m instanceof DBusSignal) {
                                List<ConnectionStruct> l;
                                synchronized (sigrecips) {
                                    l = new ArrayList<>(sigrecips);
                                }
                                for (ConnectionStruct d : l) {
                                    send(d, m);
                                }
                            } else {
                                ConnectionStruct dest = names.get(m.getDestination());

                                if (null == dest) {
                                    send(connectionStruct, messageFactory.createError(DBUS_BUSNAME, null,
                                            "org.freedesktop.DBus.Error.ServiceUnknown", m.getSerial(), "s",
                                            String.format("The name `%s' does not exist", m.getDestination())));
                                } else {
                                    send(dest, m);
                                }
                            }
                        }
                    }
                }

            } catch (DBusException _ex) {
                LOGGER.debug("Error processing connection", _ex);
            } catch (InterruptedException _ex) {
                LOGGER.debug("Interrupted");
                close();
                interrupt();
            }
        }

    }

    private void handleMatchRules(Message _msg) {
        if (_msg instanceof MethodCall mc && mc.getDestination() == null
            || !(_msg instanceof MethodCall)) {

            List<ConnectionStruct> l;
            synchronized (sigrecips) {
                l = new ArrayList<>(sigrecips);
            }

            for (ConnectionStruct connStruct : l) {
                if (!connStruct.rules.isEmpty()) {
                    connStruct.rules.forEach(e -> {
                        if (e.matches(_msg)) {
                            try {
                                Message clone = MessageFactory.createCloneWithNewSerial(_msg);
                                send(connStruct, clone);
                            } catch (Exception _ex) {
                                LOGGER.error("Error cloning message for rule matching: {}", _msg, _ex);
                            }
                        }
                    });
                }
            }
        }
    }

    private static void logMessage(String _logStr, Message _m, String _connUniqueId) {
        Object logMsg = _m;
        if (_m != null && Introspectable.class.getName().equals(_m.getInterface()) && !LOGGER.isTraceEnabled()) {
            logMsg = "<Introspection data only visible in loglevel trace>";
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(_logStr, logMsg, _connUniqueId);
        } else {
            LOGGER.debug(_logStr, _m, _connUniqueId);
        }
    }

    public synchronized boolean isRunning() {
        return run.get();
    }

    @Override
    public void close() {
        run.set(false);
        if (!conns.isEmpty()) {
            // disconnect all remaining connections
            Set<ConnectionStruct> connections = new HashSet<>(conns.keySet());
            for (ConnectionStruct c : connections) {
                removeConnection(c);
            }
        }
        sender.terminate();
        if (transport != null) {
            LOGGER.debug("Terminating transport {}", transport);
            try {
                // shutdown listener
                transport.close();
            } catch (IOException _ex) {
                LOGGER.debug("Error closing transport", _ex);
            }
        }
        interrupt();
    }

    private void removeConnection(ConnectionStruct _c) {

        DBusDaemonReaderThread oldThread = conns.remove(_c);

        if (oldThread != null) {
            LOGGER.debug("Terminating reader thread for {}", _c);
            oldThread.terminate();

            try {
                if (_c.connection != null) {
                    _c.connection.close();
                    LOGGER.debug("Terminated connection {}", _c.connection);
                }
            } catch (IOException _exIo) {
                LOGGER.debug("Error while closing socketchannel", _exIo);
            }
        }

        LOGGER.debug("Removing signal destination {}", _c);
        synchronized (sigrecips) {
            if (sigrecips.removeIf(e -> e.equals(_c))) {
                LOGGER.debug("Removed one or more signal destinations for {}", _c);
            }
        }

        LOGGER.debug("Removing name registration for {}", _c);
        synchronized (names) {
            List<String> toRemove = new ArrayList<>();

            // find connection by name
            for (Entry<String, ConnectionStruct> e : names.entrySet()) {
                if (e.getValue() == _c) {
                    toRemove.add(e.getKey());
                }
            }

            // remove registered name and send signal to remaining connections
            for (String name : toRemove) {
                names.remove(name);
                try {
                    send(null, new NameOwnerChanged(DBUS_BUSPATH, name, _c.unique, ""));
                } catch (DBusException _ex) {
                    LOGGER.debug("Unable to change owner", _ex);
                }
            }
        }

    }

    void addSock(TransportConnection _s) {
        LOGGER.debug("New Client");

        ConnectionStruct c = new ConnectionStruct(_s);
        DBusDaemonReaderThread r = new DBusDaemonReaderThread(c);
        conns.put(c, r);
        r.start();
    }

    public static void syntax() {
        System.out.println("Syntax: DBusDaemon [--version] [-v] [--help] [-h] [--listen address] "
                + "[-l address] [--print-address] [-r] [--pidfile file] [-p file] [--addressfile file] "
                + "[--auth-mode AUTH_ANONYMOUS|AUTH_COOKIE|AUTH_EXTERNAL] [-m AUTH_ANONYMOUS|AUTH_COOKIE|AUTH_EXTERNAL]"
                + "[-a file] [--unix] [-u] [--tcp] [-t] ");
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
        String authModeStr = null;
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
                } else if ("--auth-mode".equals(_args[i]) || "-m".equals(_args[i])) {
                    authModeStr = _args[++i];
                } else {
                    syntax();
                }
            }
        } catch (ArrayIndexOutOfBoundsException _ex) {
            syntax();
        }

        // generate a random address if none specified
        if (null == addr && unix) {
            addr = TransportBuilder.createDynamicSession("UNIX", true);
        } else if (null == addr && tcp) {
            addr = TransportBuilder.createDynamicSession("TCP", true);
        }

        BusAddress address = BusAddress.of(addr);

        // print address to stdout
        if (printaddress) {
            System.out.println(addr);
        }

        SaslAuthMode saslAuthMode = null;
        if (authModeStr != null) {
            String selectedMode = authModeStr;
            saslAuthMode = Arrays.stream(SaslAuthMode.values())
                    .filter(e -> e.name().toLowerCase().matches(selectedMode.toLowerCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Auth mode '" + selectedMode + "' unsupported"));
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
            daemon.setSaslAuthMode(saslAuthMode);
            daemon.startInForeground();
        }

    }

    public static class ConnectionStruct {
        private final TransportConnection       connection;
        private String                          unique;
        private final Set<DBusMatchRule>        rules = Collections.synchronizedSet(new LinkedHashSet<>());

        ConnectionStruct(TransportConnection _c) {
            connection = _c;
        }

        @Override
        public String toString() {
            return null == unique ? ":?-?" : unique;
        }
    }

    public class DBusServer implements DBus, Introspectable, Peer {

        private final String machineId;
        private ConnectionStruct connStruct;

        public DBusServer() {
            machineId = AddressBuilder.createMachineId();
        }

        /**
         * Create a 'NameAcquired' signal manually.<br>
         * This is required because the implementation in DBusNameAquired is for receiving of this signal only.
         *
         * @param _connection connection
         * @param _name name to announce
         *
         * @return signal
         * @throws DBusException if signal creation fails
         */
        private DBusSignal generateNameAcquiredSignal(TransportConnection _connection, String _name) throws DBusException {
            return _connection.getMessageFactory().createSignal(DBUS_BUSNAME, DBUS_BUSPATH, DBUS_BUSNAME, "NameAcquired", "s", _name);
        }

        /**
         * Create a 'NameOwnerChanged' signal manually. <br>
         * This is required because the implementation in DBusNameAquired is for receiving of this signal only.
         *
         * @param _connection connection
         * @param _name name to announce
         * @param _oldOwner previous owner
         * @param _newOwner new owner
         *
         * @return signal
         * @throws DBusException if signal creation fails
         */
        private DBusSignal generatedNameOwnerChangedSignal(TransportConnection _connection, String _name, String _oldOwner, String _newOwner) throws DBusException {
            return _connection.getMessageFactory().createSignal(DBUS_BUSNAME, DBUS_BUSPATH, DBUS_BUSNAME, "NameOwnerChanged", "sss", _name, _oldOwner, _newOwner);
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
                connStruct.unique = ":1." + nextUnique.incrementAndGet();
            }
            names.put(connStruct.unique, connStruct);

            LOGGER.info("Client {} registered", connStruct.unique);

            try {
                send(connStruct, generateNameAcquiredSignal(connStruct.connection, connStruct.unique));
                send(null, generatedNameOwnerChangedSignal(connStruct.connection, connStruct.unique, "", connStruct.unique));
            } catch (DBusException _ex) {
                LOGGER.debug("", _ex);
            }

            return connStruct.unique;
        }

        @Override
        public String[] ListNames() {
            String[] ns;
            Set<String> nss = names.keySet();
            ns = nss.toArray(EMPTY_STRING_ARRAY);
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
        @SuppressWarnings("checkstyle:innerassignment")
        public UInt32 RequestName(String _name, UInt32 _flags) {
            boolean exists = false;
            synchronized (names) {
                if (!(exists = names.containsKey(_name))) {
                    names.put(_name, connStruct);
                }
            }

            int rv;
            if (exists) {
                rv = DBUS_REQUEST_NAME_REPLY_EXISTS;
            } else {

                LOGGER.info("Client {} acquired name {}", connStruct.unique, _name);

                rv = DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER;
                try {
                    send(connStruct, generateNameAcquiredSignal(connStruct.connection, _name));
                    send(null, generatedNameOwnerChangedSignal(connStruct.connection, _name, "", connStruct.unique));
                } catch (DBusException _ex) {
                    LOGGER.debug("", _ex);
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
                rv = DBUS_RELEASE_NAME_REPLY_NON_EXISTANT;
            } else {
                LOGGER.info("Client {} acquired name {}", connStruct.unique, _name);
                rv = DBUS_RELEASE_NAME_REPLY_RELEASED;
                try {
                    send(connStruct, new NameLost(DBUS_BUSPATH, _name));
                    send(null, new NameOwnerChanged(DBUS_BUSPATH, _name, connStruct.unique, ""));
                } catch (DBusException _ex) {
                    LOGGER.debug("", _ex);
                }
            }

            return new UInt32(rv);
        }

        @Override
        public void AddMatch(String _matchrule) throws MatchRuleInvalid {

            LOGGER.trace("Adding match rule: {}", _matchrule);

            DBusMatchRule matchRule = MatchRuleParser.convertMatchRule(_matchrule);
            if (matchRule == null) {
                throw new MatchRuleInvalid("Received invalid match rule " + _matchrule);
            }

            synchronized (sigrecips) {
                if (!sigrecips.contains(connStruct)) {
                    sigrecips.add(connStruct);
                }
            }

            synchronized (connStruct.rules) {
                connStruct.rules.add(matchRule);
            }
        }

        @Override
        public void RemoveMatch(String _matchrule) throws MatchRuleInvalid {
            LOGGER.trace("Removing match rule: {}", _matchrule);
            DBusMatchRule matchRule = MatchRuleParser.convertMatchRule(_matchrule);
            if (matchRule == null) {
                throw new MatchRuleInvalid("Received invalid match rule " + _matchrule);
            }

            synchronized (connStruct.rules) {
                connStruct.rules.remove(matchRule);
            }
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
            MessageFactory messageFactory = _connStruct.connection.getMessageFactory();

            try {
                meth = DBusServer.class.getMethod(_msg.getName(), cs);
                try {
                    this.connStruct = _connStruct;
                    rv = meth.invoke(dbusServer, args);
                    if (null == rv) {
                        send(_connStruct, messageFactory.createMethodReturn(DBUS_BUSNAME, (MethodCall) _msg, null), true);
                    } else {
                        String sig = Marshalling.getDBusType(meth.getGenericReturnType())[0];
                        send(_connStruct, messageFactory.createMethodReturn(DBUS_BUSNAME, (MethodCall) _msg, sig, rv), true);
                    }
                } catch (InvocationTargetException _exIte) {
                    LOGGER.debug("", _exIte);
                    send(_connStruct, messageFactory.createError(DBUS_BUSNAME, _msg, _exIte.getCause()));
                } catch (DBusExecutionException _exDnEe) {
                   LOGGER.debug("", _exDnEe);
                   send(_connStruct, messageFactory.createError(DBUS_BUSNAME, _msg, _exDnEe));
                } catch (Exception _ex) {
                    LOGGER.debug("", _ex);
                    send(_connStruct, messageFactory.createError(DBUS_BUSNAME, _connStruct.unique,
                            "org.freedesktop.DBus.Error.GeneralError", _msg.getSerial(), "s", "An error occurred while calling " + _msg.getName()));
                }
            } catch (NoSuchMethodException _exNsm) {
                send(_connStruct, messageFactory.createError(DBUS_BUSNAME, _connStruct.unique,
                        "org.freedesktop.DBus.Error.UnknownMethod", _msg.getSerial(), "s", "This service does not support " + _msg.getName()));
            }

        }

        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public String Introspect() {
            return """
                <!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN"
                "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
                <node>
                  <interface name="org.freedesktop.DBus.Introspectable">
                    <method name="Introspect">
                      <arg name="data" direction="out" type="s"/>
                    </method>
                  </interface>
                  <interface name="org.freedesktop.DBus">
                    <method name="RequestName">
                      <arg direction="in" type="s"/>
                      <arg direction="in" type="u"/>
                      <arg direction="out" type="u"/>
                    </method>
                    <method name="ReleaseName">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="u"/>
                    </method>
                    <method name="StartServiceByName">
                      <arg direction="in" type="s"/>
                      <arg direction="in" type="u"/>
                      <arg direction="out" type="u"/>
                    </method>
                    <method name="Hello">
                      <arg direction="out" type="s"/>
                    </method>
                    <method name="NameHasOwner">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="b"/>
                    </method>
                    <method name="ListNames">
                      <arg direction="out" type="as"/>
                    </method>
                    <method name="ListActivatableNames">
                      <arg direction="out" type="as"/>
                    </method>
                    <method name="AddMatch">
                      <arg direction="in" type="s"/>
                    </method>
                    <method name="RemoveMatch">
                      <arg direction="in" type="s"/>
                    </method>
                    <method name="GetNameOwner">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="s"/>
                    </method>
                    <method name="ListQueuedOwners">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="as"/>
                    </method>
                    <method name="GetConnectionUnixUser">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="u"/>
                    </method>
                    <method name="GetConnectionUnixProcessID">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="u"/>
                    </method>
                    <method name="GetConnectionSELinuxSecurityContext">
                      <arg direction="in" type="s"/>
                      <arg direction="out" type="ay"/>
                    </method>
                    <method name="ReloadConfig">
                    </method>
                    <signal name="NameOwnerChanged">
                      <arg type="s"/>
                      <arg type="s"/>
                      <arg type="s"/>
                    </signal>
                    <signal name="NameLost">
                      <arg type="s"/>
                    </signal>
                    <signal name="NameAcquired">
                      <arg type="s"/>
                    </signal>
                  </interface>
                </node>""";
        }

        @Override
        public void Ping() {
            // no-op
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
            // no-op
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
        private final AtomicBoolean running = new AtomicBoolean(false); // switch running status when thread begins

        public DBusDaemonSenderThread() {
            setName(getClass().getSimpleName().replace('$', '-'));
        }

        @Override
        public void run() {
            logger.debug(">>>> Sender thread started <<<<");
            running.set(true);
            while (isRunning() && running.get()) {

                logger.trace("Acquiring lock on outqueue and blocking for data");

                // block on outqueue
                try {
                    Pair<Message, WeakReference<ConnectionStruct>> pollFirst = outqueue.take();
                    if (pollFirst != null) {
                        ConnectionStruct connectionStruct = pollFirst.second.get();
                        if (connectionStruct != null) {
                            if (connectionStruct.connection.getChannel().isConnected()) {
                                logger.debug("<outqueue> Got message {} for {}", pollFirst.first, connectionStruct.unique);

                                try {
                                    connectionStruct.connection.getWriter().writeMessage(pollFirst.first);
                                } catch (IOException _ex) {
                                    logger.debug("Disconnecting client due to previous exception", _ex);
                                    removeConnection(connectionStruct);
                                }
                            } else {
                                logger.warn("Connection to {} broken", connectionStruct.connection);
                                removeConnection(connectionStruct);
                            }

                        } else {
                            logger.info("Discarding {} connection reaped", pollFirst.first);
                        }
                    }
                } catch (InterruptedException _ex) {
                    logger.debug("Got interrupted", _ex);
                    currentThread().interrupt();
                }
            }
            logger.debug(">>>> Sender Thread terminated <<<<");
        }

        public synchronized void terminate() {
            running.set(false);
            interrupt();
        }
    }

    public class DBusDaemonReaderThread extends Thread {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private ConnectionStruct                      conn;
        private final WeakReference<ConnectionStruct> weakconn;
        private final AtomicBoolean running = new AtomicBoolean(false);

        public DBusDaemonReaderThread(ConnectionStruct _conn) {
            this.conn = _conn;
            weakconn = new WeakReference<>(_conn);
            setName(getClass().getSimpleName());
        }

        public void terminate() {
            running.set(false);
        }

        @Override
        public void run() {
            logger.debug(">>>> Reader Thread started <<<<");
            running.set(true);
            while (isRunning() && running.get()) {

                Message m = null;
                try {
                    m = conn.connection.getReader().readMessage();
                } catch (IOException _ex) {
                    LOGGER.debug("Error reading message", _ex);
                    removeConnection(conn);
                } catch (DBusException _ex) {
                    LOGGER.debug("", _ex);
                    if (_ex instanceof FatalException) {
                        removeConnection(conn);
                    }
                }

                if (null != m) {
                    logMessage("Read {} from {}", m, conn.unique);

                    inqueue.add(new Pair<>(m, weakconn));
                }
            }
            conn = null;
            logger.debug(">>>> Reader Thread terminated <<<<");
        }
    }

    static class Pair<A, B> {
        private final A first;
        private final B second;

        Pair(A _first, B _second) {
            first = _first;
            second = _second;
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public boolean equals(Object _obj) {
            if (this == _obj) {
                return true;
            }
            if (!(_obj instanceof Pair<?, ?> other)) {
                return false;
            }
            return Objects.equals(first, other.first) && Objects.equals(second, other.second);
        }

    }
}
