package org.freedesktop.dbus.connections.impl;

import static org.freedesktop.dbus.utils.CommonRegexPattern.IFACE_PATTERN;
import static org.freedesktop.dbus.utils.CommonRegexPattern.PROXY_SPLIT_PATTERN;

import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.config.ReceivingServiceConfig;
import org.freedesktop.dbus.connections.config.TransportConfig;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;
import org.freedesktop.dbus.exceptions.MissingInterfaceImplementationException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.utils.AddressBuilder;
import org.freedesktop.dbus.utils.DBusObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles a peer to peer connection between two applications without a bus daemon.
 * <p>
 * Signal Handlers and method calls from remote objects are run in their own threads, you MUST handle the concurrency issues.
 * </p>
 */
public class DirectConnection extends AbstractConnection {
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String machineId;

    DirectConnection(ConnectionConfig _conCfg, TransportConfig _transportCfg, ReceivingServiceConfig _rsCfg) throws DBusException {
        super(_conCfg, _transportCfg, _rsCfg);
        machineId = AddressBuilder.createMachineId();
        if (!getAddress().isServer()) {
            try {
                listen();
            } catch (IOException _ex) {
                throw new DBusException(_ex);
            }
        }
    }

    /**
     * Use this method when running on server side.
     * <p>
     * Call will block.
     * </p>
     *
     * @throws IOException when connection fails
     */
    @Override
    public void listen() throws IOException {
        if (getAddress().isServer()) {
            getTransport().listen();
        }
        super.listen();
    }

    @SuppressWarnings("unchecked")
    <T extends DBusInterface> T dynamicProxy(String _path, Class<T> _type) throws DBusException {
        try {
            Introspectable intro = getRemoteObject(_path, Introspectable.class);
            String data = intro.Introspect();

            String[] tags = PROXY_SPLIT_PATTERN.split(data);

            List<String> ifaces = Arrays.stream(tags).filter(t -> t.startsWith("interface"))
                .map(t -> IFACE_PATTERN.matcher(t).replaceAll("$1"))
                .toList();

            List<Class<?>> ifcs = findMatchingTypes(_type, ifaces);

            if (ifcs.isEmpty()) {
                throw new DBusException("Could not find an interface to cast to");
            }

            RemoteObject ro = new RemoteObject(null, _path, _type, false);
            DBusInterface newi = (DBusInterface) Proxy.newProxyInstance(ifcs.getFirst().getClassLoader(), ifcs.toArray(EMPTY_CLASS_ARRAY), new RemoteInvocationHandler(this, ro));
            getImportedObjects().put(newi, ro);
            return (T) newi;
        } catch (Exception _ex) {
            logger.debug("Error creating dynamic proxy", _ex);
            throw new DBusException(String.format("Failed to create proxy object for %s; reason: %s.", _path, _ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    <T extends DBusInterface> T getExportedObject(String _path, Class<T> _type) throws DBusException {
        ExportedObject o = doWithExportedObjectsAndReturn(DBusException.class, eos -> eos.get(_path));

        if (null != o && null == o.getObject().get()) {
            unExportObject(_path);
            o = null;
        }
        if (null != o) {
            return (T) o.getObject().get();
        }
        return dynamicProxy(_path, _type);
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
       * @param _objectPath The path on which the process is exporting the object.
       * @return A reference to a remote object.
       * @throws ClassCastException If type is not a sub-type of DBusInterface
       * @throws DBusException If busname or objectpath are incorrectly formatted.
    */
    public DBusInterface getRemoteObject(String _objectPath) throws DBusException {
        if (null == _objectPath) {
            throw new DBusException("Invalid object path: null");
        }

        DBusObjects.requireObjectPath(_objectPath);

        return dynamicProxy(_objectPath, null);
    }

    /**
       * Return a reference to a remote object.
       * This method will always refer to the well known name (if given) rather than resolving it to a unique bus name.
       * In particular this means that if a process providing the well known name disappears and is taken over by another process
       * proxy objects gained by this method will make calls on the new proccess.
       * @param _objectPath The path on which the process is exporting the object.
       * @param _type The interface they are exporting it on. This type must have the same full class name and exposed method signatures
       * as the interface the remote object is exporting.
       * @param <T> class which extends DBusInterface
       * @return A reference to a remote object.
       * @throws MissingInterfaceImplementationException If type is not a sub-type of DBusInterface
       * @throws InvalidObjectPathException If busname or objectpath are invalid
       *
    */
    public <T extends DBusInterface> T getRemoteObject(String _objectPath, Class<T> _type) throws DBusException {
        DBusObjects.requireObjectPath(_objectPath);
        DBusObjects.requireDBusInterface(_type);
        DBusObjects.requirePackage(_type);

        RemoteObject ro = new RemoteObject(null, _objectPath, _type, false);

        @SuppressWarnings("unchecked")
        T i = (T) Proxy.newProxyInstance(_type.getClassLoader(),
                new Class[] {_type}, new RemoteInvocationHandler(this, ro));

        getImportedObjects().put(i, ro);

        return i;
    }

    @Override
    public <T extends DBusSignal> void removeSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException {
        Queue<DBusSigHandler<? extends DBusSignal>> v = getHandledSignals().get(_rule);
        if (v != null) {
            v.remove(_handler);
            if (v.isEmpty()) {
                getHandledSignals().remove(_rule);
            }
        }
    }

    @Override
    public <T extends DBusSignal> AutoCloseable addSigHandler(DBusMatchRule _rule, DBusSigHandler<T> _handler) throws DBusException {
        Queue<DBusSigHandler<? extends DBusSignal>> v =
                getHandledSignals().computeIfAbsent(_rule, val -> new ConcurrentLinkedQueue<>());

        v.add(_handler);
        return () -> removeSigHandler(_rule, _handler);
    }

    @Override
    protected void removeGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException {
        Queue<DBusSigHandler<DBusSignal>> v = getGenericHandledSignals().get(_rule);
        if (v != null) {
            v.remove(_handler);
            if (v.isEmpty()) {
                getGenericHandledSignals().remove(_rule);
            }
        }
    }

    @Override
    protected AutoCloseable addGenericSigHandler(DBusMatchRule _rule, DBusSigHandler<DBusSignal> _handler) throws DBusException {
        Queue<DBusSigHandler<DBusSignal>> v =
                getGenericHandledSignals().computeIfAbsent(_rule, val -> new ConcurrentLinkedQueue<>());

        v.add(_handler);
        return () -> removeGenericSigHandler(_rule, _handler);
    }

    @Override
    public <T extends DBusInterface> T getExportedObject(String _source, String _path, Class<T> _type) throws DBusException {
        return getExportedObject(_path, _type);
    }

    @Override
    public String getMachineId() {
       return machineId;
    }

    @Override
    public DBusInterface getExportedObject(String _source, String _path) throws DBusException {
        return getExportedObject(_path, (Class<DBusInterface>) null);
    }
}
