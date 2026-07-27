package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

/**
 * A ready-to-use {@link ObjectManager} implementation exported by
 * {@link DBusConnection#exportObjectManager(String)}.
 * <p>
 * Its {@code GetManagedObjects} is answered automatically by the connection (by enumerating the exported
 * sub-tree), so the implementation here is just a placeholder. It exists to register the ObjectManager at
 * a given object path without requiring the application to write its own class.
 * </p>
 *
 * @since 6.0.0 - 2026-07-18
 */
public class DBusObjectManager implements ObjectManager {

    private final String objectPath;

    public DBusObjectManager(String _objectPath) {
        objectPath = _objectPath;
    }

    /**
     * Placeholder - the connection answers GetManagedObjects automatically by enumerating the sub-tree.
     */
    @Override
    public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects() {
        return Map.of();
    }

    @Override
    public String getObjectPath() {
        return objectPath;
    }

    @Override
    public boolean isRemote() {
        return false;
    }
}
