package org.freedesktop.dbus.test;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.junit.jupiter.api.Test;

public class SignalNameTest extends AbstractBaseTest {
    @DBusInterfaceName("d.e.f.Custom")
    public interface CustomService extends DBusInterface {
        void nothing();

        @DBusMemberName("custom_signal")
        class CustomSignal extends DBusSignal {
            public final String data;

            public CustomSignal(String path, String data) throws DBusException {
                super(path, data);
                this.data = data;
            }
        }
    }

    public static class MyCustomImpl implements CustomService {
        @Override
        public void nothing() {
            System.out.println("Just doing nothing");
        }

        @Override
        public String getObjectPath() {
            return "/d/e/f/custom";
        }
    }

    /**
     * This test will fail when the signal in CustomService cannot be
     * created using the name found in DBusInterfaceName annotation.
     * <br>
     * Purpose is to detect missing DBus interface aliases when exporting objects to the bus.
     * <br>
     * @see <a href="https://github.com/hypfvieh/dbus-java/issues/186">Issue 186</a>
     *
     * @throws Exception
     */
    @Test
    public void testSignalNameAlias() throws Exception {
        String protocolType = TransportBuilder.getRegisteredBusTypes().get(0);
        BusAddress busAddress = TransportBuilder.
                createWithDynamicSession(protocolType)
                .configure().build().getBusAddress();

        BusAddress listenBusAddress = BusAddress.of(busAddress).getListenerAddress();

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackground();
            logger.debug("Started embedded bus on address {}", listenBusAddress);

            waitForDaemon(daemon);

            // connect to started daemon process
            logger.info("Connecting to embedded DBus {}", busAddress);


            try (DBusConnection connection = DBusConnectionBuilder.forAddress(busAddress).build()) {
                connection.requestBusName("d.e.f.Service");
                connection.exportObject("/d/e/f/custom", new MyCustomImpl());

                connection.addSigHandler(CustomService.CustomSignal.class, new DBusSigHandler<CustomService.CustomSignal>() {
                    @Override
                    public void handle(CustomService.CustomSignal s) {
                        System.out.printf("Received signal: %s%n", s.data);
                    }
                });

                connection.sendMessage(new CustomService.CustomSignal("/a/b/c/custom", "hello world"));
                // wait to deliver message
                Thread.sleep(1000);

            }
        }
    }
}
