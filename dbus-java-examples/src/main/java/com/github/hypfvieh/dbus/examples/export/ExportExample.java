package com.github.hypfvieh.dbus.examples.export;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

import java.util.concurrent.CountDownLatch;

/**
 * Sample code to demonstrate exporting of an object.<br>
 * This is the code also found in {@code export-objects.md}
 *
 * @author hypfvieh
 * @since 5.0.0 - 2023-10-04
 */
public class ExportExample implements ISampleExport {

    private final DBusConnection dbusConn;
    private final CountDownLatch waitClose;

    ExportExample() throws DBusException, InterruptedException {
        waitClose = new CountDownLatch(1);
        // Get a connection to the session bus so we can request a bus name
        dbusConn = DBusConnectionBuilder.forSessionBus().build();
        // Request a unique bus name
        dbusConn.requestBusName("test.dbusjava.export");
        // Export this object onto the bus using the path '/'
        dbusConn.exportObject(getObjectPath(), this);
        // this will cause the countdown latch to wait until terminateApp() was called
        // you probably want to do something more useful
        waitClose.await();
        System.out.println("bye bye");
    }

    @Override
    public int add(int _a, int _b) {
        return _a + _b;
    }

    @Override
    public void terminateApp() {
        waitClose.countDown();
    }

    @Override
    public boolean isRemote() {
        /* Whenever you are implementing an object, always return false */
        return false;
    }

    @Override
    public String getObjectPath() {
        /*
         * This is not strictly needed; it is a convenience method for housekeeping on your application side if you will
         * be exporting and unexporting many times
         */
        return "/";
    }

    public static void main(String[] _args) throws Exception {
        new ExportExample();
    }

}
