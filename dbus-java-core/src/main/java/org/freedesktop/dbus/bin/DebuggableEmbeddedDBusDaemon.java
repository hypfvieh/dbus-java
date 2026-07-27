package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.InvalidBusAddressException;

/**
 * Variant of {@link EmbeddedDBusDaemon} which additionally offers the {@code org.freedesktop.DBus.Debug.Stats}
 * interface (see {@link org.freedesktop.dbus.interfaces.Debug.Stats}).
 * <p>
 * The reference {@code dbus-daemon} only exposes these debug/statistics interfaces when it was compiled with the
 * corresponding support enabled. To keep the default {@link EmbeddedDBusDaemon} behaving like a production daemon, the
 * debug features are only available through this dedicated subclass. Callers must knowingly decide which daemon variant
 * they want to run/develop against.
 * </p>
 * <p>
 * This daemon is intended for debugging and diagnostics only and must not be used in production.
 * </p>
 */
public class DebuggableEmbeddedDBusDaemon extends EmbeddedDBusDaemon {

    public DebuggableEmbeddedDBusDaemon(BusAddress _address) {
        super(_address);
    }

    public DebuggableEmbeddedDBusDaemon(String _address) throws InvalidBusAddressException {
        super(_address);
    }

    @Override
    protected boolean isDebugStatsEnabled() {
        return true;
    }
}
