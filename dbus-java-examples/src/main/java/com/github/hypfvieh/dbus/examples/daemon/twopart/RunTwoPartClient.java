package com.github.hypfvieh.dbus.examples.daemon.twopart;

import com.github.hypfvieh.dbus.examples.daemon.twopart.RunTwoPartDaemon.IExport;
import com.github.hypfvieh.util.FileIoUtil;
import com.github.hypfvieh.util.StringUtil;
import com.github.hypfvieh.util.SystemUtil;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

import java.io.File;
import java.io.IOException;

/**
 * Sample client which connects to our sample sample (#{@link RunTwoPartDaemon}).
 *
 * @author hypfvieh
 */
public final class RunTwoPartClient {

    private RunTwoPartClient() {}

    public static void main(String[] _args) {
        String twopartAddress = FileIoUtil.readFileToString(new File(SystemUtil.getTempDir(), "twopartdaemon.address").getAbsolutePath());
        if (StringUtil.isBlank(twopartAddress)) {
            throw new RuntimeException("No twopart daemon found");
        }

        try (DBusConnection conn = DBusConnectionBuilder.forAddress(twopartAddress).build()) {
            // repeat until JVM is killed
            while (true) {
                IExport remoteObject = conn.getRemoteObject(RunTwoPartDaemon.EXPORT_NAME, "/", IExport.class);
                System.out.println("Remote side says: " + remoteObject.sayHello());

                try {
                    Thread.sleep(500L);
                } catch (InterruptedException _ex) {
                    System.err.println("interrupted");
                    Thread.currentThread().interrupt();
                }
            }

        } catch (IOException | DBusException _ex) {
            throw new RuntimeException("Could not connect to twopart daemon");
        }
    }

}
