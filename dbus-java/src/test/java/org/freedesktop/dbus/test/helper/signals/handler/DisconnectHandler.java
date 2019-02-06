package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Local;
import org.freedesktop.dbus.test.helper.signals.SampleSignals;

/**
 * Disconnect handler
 */
public class DisconnectHandler extends AbstractSignalHandler<Local.Disconnected> {
    private DBusConnection       conn;
    private RenamedSignalHandler sh;

    public DisconnectHandler(DBusConnection _conn, RenamedSignalHandler _sh) {
        super(1);
        this.conn = _conn;
        this.sh = _sh;
    }

    /** Handling a signal */
    @Override
    public void handleImpl(Local.Disconnected t) {
        System.out.println("Disconnect Signal Received.");
        try {
            conn.removeSigHandler(SampleSignals.TestRenamedSignal.class, sh);
        } catch (DBusException ex) {
            fail("Could not remove signal handler");
        }
    }
}