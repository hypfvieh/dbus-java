package com.github.hypfvieh.dbus.examples.daemon;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * Sample on how to start the {@link EmbeddedDBusDaemon} with transport protocol selection.
 *
 * @author hypfvieh
 */
public class RunDaemon {
    /** Max wait time to wait for daemon to start. */
    private static final long MAX_WAIT = Duration.ofSeconds(30).toMillis();

    private final Logger log;
    private final String newAddress;

    private EmbeddedDBusDaemon daemon;

    public RunDaemon(TransportProtocol _transportProtocol) {
        log = LoggerFactory.getLogger(getClass());
        Objects.requireNonNull(_transportProtocol, "TransportProtocol required.");

        newAddress = TransportBuilder.createDynamicSession(_transportProtocol.name(), false);
    }

    private void startDaemon() throws DBusException {
        if (daemon == null) {

            BusAddress listenBusAddress = BusAddress.of(newAddress);
            String listenAddress = newAddress;

            if (!listenBusAddress.isListeningSocket()) {
                listenAddress = newAddress + ",listen=true";
                listenBusAddress = BusAddress.of(listenAddress);
            }

            log.info("Starting embedded bus on address {})", listenBusAddress);
            daemon = new EmbeddedDBusDaemon(listenBusAddress);
            daemon.startInBackground();
            log.info("Started embedded bus on address {}", listenBusAddress);

            Util.waitFor("EmbeddedDbusDaemon", daemon::isRunning, MAX_WAIT, 200);
        }
    }

    private void connectSelf() throws DBusException, IOException {
        BusAddress busAddress = BusAddress.of(newAddress);
        log.info("Connecting to embedded DBus {}", busAddress);
        for (int i = 0; i < 6; i++) {
            try {
                try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress).build()) {
                    log.info("Connected to embedded DBus {}", busAddress);
                    // do something with the connection ;)
                }
                break;
            } catch (DBusException _ex) {
                if (i > 4) {
                    throw _ex;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException _exIe) {
                    throw new IOException("Interrupted. ", _exIe);
                }
            }
        }
    }

    public static void main(String[] _args) throws Exception {
        TransportProtocol proto = TransportProtocol.UNIX;
        if (_args.length == 1) {
            if (_args[1].equalsIgnoreCase("TCP")) {
                proto = TransportProtocol.TCP;
            } else if (_args[1].equalsIgnoreCase("UNIX")) {
                proto = TransportProtocol.UNIX;
            } else {
                throw new RuntimeException("Unknown transport protocol: " + _args[1]);
            }
        }
        RunDaemon runDaemon = new RunDaemon(proto);
        runDaemon.startDaemon();
        runDaemon.connectSelf();
    }

    enum TransportProtocol {
        TCP,
        UNIX
    }
}
