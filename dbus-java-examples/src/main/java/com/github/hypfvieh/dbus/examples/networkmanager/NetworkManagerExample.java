package com.github.hypfvieh.dbus.examples.networkmanager;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sample code which connects to the network manager and queries devices, permissions and IPs.
 *
 * @author hypfvieh
 *
 */
public final class NetworkManagerExample {

    private NetworkManagerExample() {}

    public static void main(String[] _args) throws DBusException {

        try (DBusConnection dbusConn = DBusConnectionBuilder.forSystemBus().build()) {
            NetworkManager nm = dbusConn.getRemoteObject("org.freedesktop.NetworkManager", "/org/freedesktop/NetworkManager",
                    NetworkManager.class);

            System.out.println("");
            System.out.println("Permissions");
            System.out.println("------------------");
            System.out.println("");

            Map<CharSequence, CharSequence> getPermissions = nm.GetPermissions();
            for (Entry<CharSequence, CharSequence> entry : getPermissions.entrySet()) {
                System.out.println("Permission: " + entry.getKey() + " = " + entry.getValue());
            }

            System.out.println("");
            System.out.println("Device Information");
            System.out.println("------------------");
            System.out.println("");

            for (DBusPath path : nm.GetDevices()) {
                System.out.println("DevicePath: " + path);
                Properties deviceProps = dbusConn.getRemoteObject("org.freedesktop.NetworkManager", path.toString(), Properties.class);

                DBusPath ipv4Path = deviceProps.Get("org.freedesktop.NetworkManager.Device", "Ip4Config");
                DBusPath ipv6Path = deviceProps.Get("org.freedesktop.NetworkManager.Device", "Ip6Config");
                System.out.println("ConfigPath: " + ipv4Path);

                System.out.println("  Interface: " + deviceProps.Get("org.freedesktop.NetworkManager.Device", "Interface"));

                if (ipv4Path.toString().equals("/")) {
                    System.out.println("        IPv4: No IPv4 Config");
                } else {
                    Properties ipv4Config = dbusConn.getRemoteObject("org.freedesktop.NetworkManager", ipv4Path.toString(), Properties.class);

                    Map<String, Variant<?>> allIpV4 = ipv4Config.GetAll("org.freedesktop.NetworkManager.IP4Config");
                    for (String key : allIpV4.keySet()) {
                       System.out.println("key: " + key);
                    }

                    List<Map<String, Variant<?>>> addressArrV4 = ipv4Config.Get("org.freedesktop.NetworkManager.IP4Config", "AddressData");

                    // addressArr contains all IPs of the interface and additional properties (if any)
                    printAddressInfo(addressArrV4, "IPv4");
                }

                if (ipv6Path.toString().equals("/")) {
                    System.out.println("        IPv6: No IPv6 Config");
                } else {

                    Properties ipv6Config = dbusConn.getRemoteObject("org.freedesktop.NetworkManager", ipv6Path.toString(), Properties.class);
                    List<Map<String, Variant<?>>> addressArrV6 = ipv6Config.Get("org.freedesktop.NetworkManager.IP6Config", "AddressData");
                    printAddressInfo(addressArrV6, "IPv6");
                }
                System.out.println("");
            }

        }
    }

    private static void printAddressInfo(List<Map<String, Variant<?>>> _addressArr, String _addrType) {
        for (int i = 0; i < _addressArr.size(); i++) {
            System.out.println("  " + _addrType + " Address " + (i + 1));
            if (_addressArr.get(i).containsKey("address")) {
                System.out.print("     " + _addrType + ": " + String.valueOf(_addressArr.get(i).get("address")).replaceAll("\\[|\\]", ""));
                if (_addressArr.get(i).containsKey("prefix")) {
                    System.out.println(" / " + String.valueOf(_addressArr.get(i).get("prefix")).replaceAll("\\[|\\]", ""));
                }
            } else {
                System.out.println("");
            }
            for (Entry<String, Variant<?>> entry : _addressArr.get(i).entrySet()) {
                if (entry.getKey().equals("address") || entry.getKey().equals("prefix")) {
                    continue;
                }
                System.out.println("     " + entry.getKey() + " = " + entry.getValue());
            }
        }
    }
}
