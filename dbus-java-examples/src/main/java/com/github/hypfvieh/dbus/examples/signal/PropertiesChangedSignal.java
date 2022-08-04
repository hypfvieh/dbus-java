package com.github.hypfvieh.dbus.examples.signal;

import java.io.IOException;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;

/**
 * This example code demonstrates how to register a callback for a signal.
 * A global {@link PropertiesChanged} handler is installed, which is queried
 * for every properties changed event on the bus.
 *
 * @author hypfvieh
 */
public class PropertiesChangedSignal {

    public static void main(String[] _args) throws Exception {
        // Open connection to the system bus.
        try (DBusConnection connection = DBusConnectionBuilder.forSystemBus().build()) {
            // Add a signal handler.
            final var token = connection.addSigHandler(PropertiesChanged.class, new PropChangedHandler());

            // Pause to see events written to stdout (your code would differ).
            System.out.println("sleeping");
            Thread.sleep(60000L);
            System.out.println("shutting down");

            // Remove the signal handler.
            token.close();
        }
    }

    /**
     * This handler will be called for every property change signal.
     */
    public static class PropChangedHandler extends AbstractPropertiesChangedHandler {
        @Override
        public void handle(PropertiesChanged _s) {
            System.out.println("changed: " + _s.getPropertiesChanged());
        }
    }
}
