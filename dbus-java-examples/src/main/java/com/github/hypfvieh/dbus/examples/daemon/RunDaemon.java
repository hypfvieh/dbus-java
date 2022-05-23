package com.github.hypfvieh.dbus.examples.daemon;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            BusAddress listenBusAddress = new BusAddress(newAddress);
            String listenAddress = newAddress;

            if (!listenBusAddress.isListeningSocket()) {
                listenAddress = newAddress + ",listen=true";
                listenBusAddress = new BusAddress(listenAddress);
            }

            log.info("Starting embedded bus on address {})", listenBusAddress.getRawAddress());
            daemon = new EmbeddedDBusDaemon(listenBusAddress);
            daemon.startInBackground();
            log.info("Started embedded bus on address {}", listenBusAddress.getRawAddress());

            long sleepMs = 200;
            long waited = 0;

            while (!daemon.isRunning()) {
                if (waited >= MAX_WAIT) {
                    throw new RuntimeException("EmbeddedDbusDaemon not started in the specified time of " + MAX_WAIT + " ms");
                }

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException _ex) {
                    break;
                }

                waited += sleepMs;
                log.debug("Waiting for embedded daemon to start: {} of {} ms waited", waited, MAX_WAIT);
            }

        }
    }

    private void connectSelf() throws DBusException, IOException {
        BusAddress busAddress = new BusAddress(newAddress);
        log.info("Connecting to embedded DBus {}", busAddress.getRawAddress());
        for (int i = 0; i < 6; i++) {
            try {
                try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress.getRawAddress()).build()) {
                    log.info("Connected to embedded DBus {}", busAddress.getRawAddress());
                    // do something with the connection ;)
                }
                break;
            } catch (DBusException dbe) {
                if (i > 4) {
                    throw dbe;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted. ", e);
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
