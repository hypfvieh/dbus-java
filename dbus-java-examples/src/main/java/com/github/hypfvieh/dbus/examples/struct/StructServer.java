package com.github.hypfvieh.dbus.examples.struct;

import org.freedesktop.dbus.StructHelper;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Sample on how to use structs wrapped in Variant or not wrapped at all.
 * This is the server side.
 * @author hypfvieh
 */
public class StructServer implements IStructServer {

    private boolean method1Done = false;
    private boolean method2Done = false;

    public static void main(String[] _args) throws Exception {

        DBusConnection sessionConnection = DBusConnectionBuilder.forSessionBus().build();
        StructServer structServer = new StructServer();

        sessionConnection.requestBusName("hypfvieh.struct.Sample");

        sessionConnection.exportObject("/hypfvieh/struct/StructServer", structServer);

        System.out.println("server started");

        int i = 0;
        while (!structServer.method1Done && !structServer.method2Done) {
            Thread.sleep(1000L);
            i++;
            if (i > 100) {
                break;
            }
        }

        System.out.println("server is shutting down");

        sessionConnection.close();
    }

    @Override
    public String getObjectPath() {
        return "/";
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public void setStructDirectly(SampleStruct _struct) {
        System.out.println("The direct struct says: " + _struct.getAnInt() + " is " + _struct.getaString());
        method1Done = true;
    }

    @Override
    public void setStructFromVariant(Variant<?> _variant) {
        try {
            SampleStruct newInstance = StructHelper.createStructFromVariant(_variant, SampleStruct.class);

            System.out.println("The variant struct says: " + newInstance.getAnInt() + " is "
                    + newInstance.getaString());
            method2Done = true;

        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException _ex) {
            LoggerFactory.getLogger(StructServer.class).error("Error", _ex);
        }
    }

}
