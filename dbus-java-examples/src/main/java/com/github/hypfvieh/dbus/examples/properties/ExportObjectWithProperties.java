package com.github.hypfvieh.dbus.examples.properties;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * Sample which demonstrates on how to use an alternative mechanism to
 * provide DBus properties directly using getter and setter methods.
 * This means you do not have to implement support yourself by
 * implementing {@link Properties#Get(String, String)} etc.
 *
 * @author brettsmith / hypfvieh
 *
 * @since 4.3.1 - 2023-09-28
 */
public final class ExportObjectWithProperties {

    private ExportObjectWithProperties() {}

    public static void main(String[] _args) throws Exception {
        try (var conn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
            // create object
            var obj = new ObjectWithProperties();
            obj.setMyProperty("My property value");

            conn.requestBusName("com.acme");

            // export to bus
            conn.exportObject(obj);

            try (DBusConnection innerConn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
                // get the exported object
                InterfaceWithProperties myObject = innerConn.getRemoteObject("com.acme", "/com/acme/ObjectWithProperties", InterfaceWithProperties.class);

                // Print hello from 'parent' object
                System.out.println("> " + myObject.sayHello());

                // Print value of property from object via its getter
                System.out.println("> " + myObject.getMyProperty());

                // Change values of property on object via their setters
                myObject.setMyProperty("A new value!");
                myObject.setMyOtherProperty(true);
                myObject.setMyAltProperty(987);

            }

            // Prove value of property on original object
            System.out.println("This should say 'A new value!': " + obj.getMyProperty());
            System.out.println("This should say 'true': " + obj.isMyOtherProperty());
            System.out.println("This should say '987': " + obj.getMyAltProperty());
        }
    }
}
