package com.github.hypfvieh.dbus.examples.mpris;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.mpris.mediaplayer2.Player;
import org.mpris.mediaplayer2.Player.Seeked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Example which will control a <b>running</b> VLC instance using DBus.
 * <p>
 * This example requires a running VLC instance in the same session as this application is running because VLC uses the
 * DBus Session Bus.
 * </p>
 *
 * @author hypfvieh
 * @since 4.3.1 - 2023-06-13
 */
public final class ControlVlcExample {

    private ControlVlcExample() {
    }

    public static void main(String[] _args) throws DBusException, InterruptedException {

        if (_args.length == 0) {
            System.err.println("Media file required as first argument");
            System.exit(1);
        }

        Path mediaFile = Path.of(_args[0]);

        if (Files.notExists(mediaFile)) {
            System.err.println("Given file does not exists: " + mediaFile);
            System.exit(1);
        }

        try (DBusConnection dbusConn = DBusConnectionBuilder.forSessionBus().build()) {
            dbusConn.addSigHandler(Seeked.class, new SeekHandler());
            Player player = dbusConn.getRemoteObject("org.mpris.MediaPlayer2.vlc", "/org/mpris/MediaPlayer2",
                Player.class);
            String fileUri = mediaFile.normalize().toUri().toString();
            System.out.println("Opening file: " + fileUri);
            player.OpenUri(fileUri);
            System.out.println("Start playing");
            player.Play();

            // seek takes microseconds
            // see: https://specifications.freedesktop.org/mpris-spec/2.2/Player_Interface.html#Method:Seek
            System.out.println("Seek to 5th second");

            long seekTime = TimeUnit.MICROSECONDS.convert(5L, TimeUnit.SECONDS);

            player.Seek(seekTime);
            Thread.sleep(500L); // wait a bit to receive the "Seeked" signal
        } catch (IOException _ex) {
            _ex.printStackTrace();
        }
    }

    public static class SeekHandler implements DBusSigHandler<Seeked> {

        @Override
        public void handle(Seeked _signal) {
            System.out.println("Got seeked signal with position: " + _signal.getTimeInUs());
        }

    }
}
