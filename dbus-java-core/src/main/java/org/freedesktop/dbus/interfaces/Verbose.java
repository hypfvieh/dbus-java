package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.annotations.DBusInterfaceName;

/**
 * The {@code org.freedesktop.DBus.Verbose} interface toggles the verbose (debug) output of the message bus.
 * <p>
 * Like {@link Debug.Stats} this interface is only offered by the reference {@code dbus-daemon} when it was compiled
 * with the corresponding debug support. In dbus-java it is only available on a message bus started with debug features
 * enabled (see the debug-enabled variant of the embedded daemon); on a default daemon calling these methods results in
 * an {@code org.freedesktop.DBus.Error.UnknownMethod} error.
 * </p>
 */
@DBusInterfaceName("org.freedesktop.DBus.Verbose")
@SuppressWarnings({"checkstyle:methodname"})
public interface Verbose extends DBusInterface {

    /**
     * Enables verbose output on the message bus.
     */
    void EnableVerbose();

    /**
     * Disables verbose output on the message bus.
     */
    void DisableVerbose();
}
