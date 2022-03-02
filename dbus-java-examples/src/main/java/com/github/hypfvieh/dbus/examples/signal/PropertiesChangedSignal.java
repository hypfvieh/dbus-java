package com.github.hypfvieh.dbus.examples.signal;

import java.io.IOException;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;

/**
 * Sample on how to register a callback for a signal.
 * <p>
 * In this case a global {@link PropertiesChanged} handler is installed.
 * It will be queried for every properties changed event on the bus.
 * 
 * @author hypfvieh
 */
public class PropertiesChangedSignal {
    
    public static void main(String[] args) throws DBusException, InterruptedException, IOException {
        // open connection to SYSTEM Bus
        try (DBusConnection connection = DBusConnectionBuilder.forSystemBus().build()) {
            // add our signal handler
            connection.addSigHandler(PropertiesChanged.class, new PropChangedHandler());
           
            // just do some sleep so you can see the events on stdout (you would probably do something else here)
            System.out.println("sleeping");
            Thread.sleep(60000L);
            System.out.println("shutting down");
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
