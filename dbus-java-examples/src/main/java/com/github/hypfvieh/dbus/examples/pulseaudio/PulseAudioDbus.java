package com.github.hypfvieh.dbus.examples.pulseaudio;

import java.io.File;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * Sample code which connects to the pulse audio system server.
 * It will fetch the server address and will then establish a connection to that address
 * to query some information about the server.
 *  
 * @author hypfvieh
 *
 */
public class PulseAudioDbus {

    public static void main(String[] _args) throws DBusException {
        DBusConnection sessionConnection = DBusConnectionBuilder.forSystemBus().build();

        Properties properties = sessionConnection.getRemoteObject("org.pulseaudio.Server", "/org/pulseaudio/server_lookup1", org.freedesktop.dbus.interfaces.Properties.class);
        String address =  properties.Get("org.PulseAudio.ServerLookup1", "Address");

        System.out.println("Found address: " + address + "\n");

        sessionConnection.disconnect();

        File file = new File(address.replace("unix:path=", ""));
        if (!file.exists()) {
            System.out.println("Extracted address " + address + " is not a valid file/unix-socket");
        } else {
            DBusConnection connection = DBusConnectionBuilder
                    .forAddress(address)
                    .withRegisterSelf(false)
                    .withShared(false)
                    .build();

            Properties core1Props = connection.getRemoteObject("org.PulseAudio.Core1", "/org/pulseaudio/core1", org.freedesktop.dbus.interfaces.Properties.class);
            System.out.println("PulseAudio Name: " + core1Props.Get("org.PulseAudio.Core1", "Name"));
            System.out.println("PulseAudio Version: " + core1Props.Get("org.PulseAudio.Core1", "Version"));

            System.out.println("-----------------------------------------");
            System.out.println("PulseAudio Hostname: " + core1Props.Get("org.PulseAudio.Core1", "Hostname"));
            System.out.println("PulseAudio Username: " + core1Props.Get("org.PulseAudio.Core1", "Username"));

            connection.disconnect();
        }

    }

}
