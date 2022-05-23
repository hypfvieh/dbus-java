package com.github.hypfvieh.dbus.examples.struct;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.types.Variant;

/**
 * Sample client connecting to struct server.
 * Will provide the "new" structs to send it to the server.
 *
 * @author hypfvieh
 */
public class StructClient {
    public static void main(String[] _args) throws Exception {
        try (DBusConnection sessionConnection = DBusConnectionBuilder.forSessionBus().build()) {

            System.out.println("getting server");
            IStructServer remoteServer = sessionConnection.getRemoteObject("hypfvieh.struct.Sample",
                    "/hypfvieh/struct/StructServer", IStructServer.class);

            System.out.println("setting struct to server");

            SampleStruct variantStruct = new SampleStruct(21, "only the half of the truth.");

            System.out.println("Testing struct with variant");
            remoteServer.setStructFromVariant(new Variant<>(variantStruct));

            SampleStruct directStruct = new SampleStruct(42, "the answer for all questions.");

            System.err.println("Testing struct directly");
            remoteServer.setStructDirectly(directStruct);


            System.out.println("closing dbus connection");
        }
    }
}
