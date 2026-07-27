package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

/**
 * Container for the {@code org.freedesktop.DBus.Debug.*} interfaces.
 * <p>
 * These interfaces are only offered by the reference {@code dbus-daemon} when it was compiled with statistics/debug
 * support enabled. They are meant purely for debugging and diagnostics and must not be relied upon in production.
 * </p>
 */
public interface Debug {

    /**
     * The {@code org.freedesktop.DBus.Debug.Stats} interface exposes statistics about the message bus and its
     * connections.
     * <p>
     * In dbus-java these methods are only available on a message bus which was explicitly started with debug features
     * enabled (see the debug-enabled variant of the embedded daemon). On a default embedded daemon - just like on a
     * production reference daemon - calling these methods results in an
     * {@code org.freedesktop.DBus.Error.UnknownMethod} error.
     * </p>
     */
    @DBusInterfaceName("org.freedesktop.DBus.Debug.Stats")
    @SuppressWarnings({"checkstyle:methodname"})
    interface Stats extends DBusInterface {

        /**
         * Returns a set of statistics about the message bus as a whole.
         * <p>
         * The exact set of keys is implementation-specific and may change between versions. Callers should treat unknown
         * keys gracefully and must not assume that a particular key is present.
         * </p>
         *
         * @return statistics keyed by name
         */
        Map<String, Variant<?>> GetStats();

        /**
         * Returns a set of statistics about a single connection, identified by any of its bus names (unique or
         * well-known).
         *
         * @param _busName bus name identifying the connection
         *
         * @return statistics keyed by name
         */
        Map<String, Variant<?>> GetConnectionStats(String _busName);

        /**
         * Returns all match rules currently registered on the bus, grouped by the unique name of the connection that
         * added them.
         *
         * @return map of unique connection name to its match rule strings
         */
        Map<String, String[]> GetAllMatchRules();
    }
}
