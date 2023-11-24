package com.github.hypfvieh.dbus.examples.daemon.twopart;

import com.github.hypfvieh.util.FileIoUtil;
import com.github.hypfvieh.util.SystemUtil;
import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * Sample for daemon usage.
 * <p>
 * Creates a and starts a DBus daemon, connects to it and exports some objects.
 * </p>
 *
 * @author hypfvieh
 */
public class RunTwoPartDaemon {
    static final String EXPORT_NAME = RunTwoPartDaemon.class.getPackageName() + ".SomeTest";

    /** Max wait time to wait for daemon to start. */
    private static final long MAX_WAIT = Duration.ofSeconds(30).toMillis();

    private final Logger log;
    private final String newAddress;

    private EmbeddedDBusDaemon daemon;

    public RunTwoPartDaemon(TransportProtocol _transportProtocol) {
        log = LoggerFactory.getLogger(getClass());
        Objects.requireNonNull(_transportProtocol, "TransportProtocol required.");

        newAddress = TransportBuilder.createDynamicSession(_transportProtocol.name(), false);
        FileIoUtil.writeTextFile(new File(SystemUtil.getTempDir(), "twopartdaemon.address").getAbsolutePath(), newAddress, false);
    }

    public void startDaemon() {
        if (daemon == null) {

            BusAddress listenBusAddress = BusAddress.of(newAddress);

            if (!listenBusAddress.isListeningSocket()) {
                String listenAddress = newAddress + ",listen=true";
                listenBusAddress = BusAddress.of(listenAddress);
            }

            log.info("Starting embedded bus on address {})", listenBusAddress);
            daemon = new EmbeddedDBusDaemon(listenBusAddress);

            // run daemon in non-daemon thread so application will not quit
            new Thread(daemon::startInForeground).start();

            Util.waitFor("EmbeddedDbusDaemon", daemon::isRunning, MAX_WAIT, 200);
            log.info("Started embedded bus on address {}", listenBusAddress);
        }
    }

    /**
     * Create the connection to the bus to export something.
     * The exported object should be passed in by the provided function which in turn will receive the created connected.
     *
     * @param _objFunc function to supply object to export
     * @param _exportBusName name to acquire on the bus
     * @param _exportPath path for exporting object
     * @throws DBusException when DBus operation fails
     * @throws IOException when connection fails
     */
    public void connectSelf(Function<AbstractConnection, DBusInterface> _objFunc, String _exportBusName, String _exportPath) throws DBusException, IOException {
        BusAddress busAddress = BusAddress.of(newAddress);
        log.info("Connecting to embedded DBus {}", busAddress);
        try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress).build()) {
            log.info("Connected to embedded DBus {}", busAddress);

            conn.requestBusName(_exportBusName);
            conn.exportObject(_exportPath, _objFunc.apply(conn));

            // wait for clients
            while (true) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException _ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void connectSelf() throws DBusException, IOException {
        connectSelf(x -> new SomeExport(), EXPORT_NAME, "/");
    }

    public String getNewAddress() {
        return newAddress;
    }

    public static void main(String[] _args) throws Exception {
        TransportProtocol proto = TransportProtocol.UNIX;
        if (_args.length == 1) {
            if (_args[0].equalsIgnoreCase("TCP")) {
                proto = TransportProtocol.TCP;
            } else if (_args[0].equalsIgnoreCase("UNIX")) {
                proto = TransportProtocol.UNIX;
            } else {
                throw new RuntimeException("Unknown transport protocol: " + _args[0]);
            }
        }
        RunTwoPartDaemon runDaemon = new RunTwoPartDaemon(proto);
        runDaemon.startDaemon();
        runDaemon.connectSelf();
    }

    public enum TransportProtocol {
        TCP,
        UNIX
    }

    public static class SomeExport implements IExport {

        @Override
        public String sayHello() {
            return "Hello";
        }

        @Override
        public String getObjectPath() {
            return null;
        }

    }

    public interface IExport extends DBusInterface {
        String sayHello();
    }
}
