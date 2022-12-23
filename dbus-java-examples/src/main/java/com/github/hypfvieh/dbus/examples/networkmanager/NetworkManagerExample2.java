package com.github.hypfvieh.dbus.examples.networkmanager;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.networkmanager.device.Wireless;

import java.io.IOException;
import java.util.List;

public final class NetworkManagerExample2 {

    private NetworkManagerExample2() {}

    public static void main(String[] _args) {
        try (DBusConnection dbusConn = DBusConnectionBuilder.forSystemBus().build()) {

            Wireless wifiAdaptor = dbusConn.getRemoteObject("org.freedesktop.NetworkManager",
                    "/org/freedesktop/NetworkManager/Devices/0", Wireless.class);

            List<DBusPath> getAllAccessPoints = wifiAdaptor.GetAllAccessPoints();
            System.out.println(getAllAccessPoints);

        } catch (IOException | DBusException _ex) {
            _ex.printStackTrace();
        }
    }
}
