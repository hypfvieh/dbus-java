package org.freedesktop.dbus.connections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add addresses of peers to a set which will watch for them to
 * disappear and automatically remove them from the set.
 */
public class PeerSet implements Set<String>, DBusSigHandler<DBus.NameOwnerChanged> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Set<String> addresses;

    public PeerSet(DBusConnection _connection) {
        addresses = new TreeSet<String>();
        try {
            _connection.addSigHandler(new DBusMatchRule(DBus.NameOwnerChanged.class, null, null), this);
        } catch (DBusException dbe) {
            logger.debug("", dbe);
        }
    }

    @Override
    public void handle(DBus.NameOwnerChanged noc) {
        logger.debug("Received NameOwnerChanged(" + noc.name + "," + noc.oldOwner + "," + noc.newOwner + ")");
        if ("".equals(noc.newOwner) && addresses.contains(noc.name)) {
            remove(noc.name);
        }
    }

    @Override
    public boolean add(String address) {
        logger.debug("Adding " + address);
        synchronized (addresses) {
            return addresses.add(address);
        }
    }

    @Override
    public boolean addAll(Collection<? extends String> _addresses) {
        synchronized (this.addresses) {
            return this.addresses.addAll(_addresses);
        }
    }

    @Override
    public void clear() {
        synchronized (addresses) {
            addresses.clear();
        }
    }

    @Override
    public boolean contains(Object o) {
        return addresses.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> os) {
        return addresses.containsAll(os);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PeerSet) {
            return ((PeerSet) o).addresses.equals(addresses);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return addresses.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return addresses.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return addresses.iterator();
    }

    @Override
    public boolean remove(Object o) {
        logger.debug("Removing " + o);
        synchronized (addresses) {
            return addresses.remove(o);
        }
    }

    @Override
    public boolean removeAll(Collection<?> os) {
        synchronized (addresses) {
            return addresses.removeAll(os);
        }
    }

    @Override
    public boolean retainAll(Collection<?> os) {
        synchronized (addresses) {
            return addresses.retainAll(os);
        }
    }

    @Override
    public int size() {
        return addresses.size();
    }

    @Override
    public Object[] toArray() {
        synchronized (addresses) {
            return addresses.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        synchronized (addresses) {
            return addresses.toArray(a);
        }
    }
}