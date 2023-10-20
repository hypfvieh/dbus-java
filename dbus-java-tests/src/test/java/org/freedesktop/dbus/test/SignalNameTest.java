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
import org.slf4j.LoggerFactory;

public class SignalNameTest extends AbstractBaseTest {

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
            daemon.startInBackgroundAndWait(MAX_WAIT);
            logger.debug("Started embedded bus on address {}", listenBusAddress);

            // connect to started daemon process
            logger.info("Connecting to embedded DBus {}", busAddress);

            try (DBusConnection connection = DBusConnectionBuilder.forAddress(busAddress).build()) {
                connection.requestBusName("d.e.f.Service");
                connection.exportObject("/d/e/f/custom", new MyCustomImpl());

                connection.addSigHandler(CustomService.CustomSignal.class, new DBusSigHandler<CustomService.CustomSignal>() {
                    @Override
                    public void handle(CustomService.CustomSignal _s) {
                        logger.debug("Received signal: {}", _s.data);
                    }
                });

                connection.sendMessage(new CustomService.CustomSignal("/a/b/c/custom", "hello world"));
                // wait to deliver message
                Thread.sleep(1000);

            }
        }
    }

    @DBusInterfaceName("d.e.f.Custom")
    public interface CustomService extends DBusInterface {
        void nothing();

        @DBusMemberName("custom_signal")
        class CustomSignal extends DBusSignal {
            //CHECKSTYLE:OFF
            public final String data;
            //CHECKSTYLE:ON

            public CustomSignal(String _path, String _data) throws DBusException {
                super(_path, _data);
                this.data = _data;
            }
        }
    }

    public static class MyCustomImpl implements CustomService {
        @Override
        public void nothing() {
            LoggerFactory.getLogger(getClass()).debug("Just doing nothing");
        }

        @Override
        public String getObjectPath() {
            return "/d/e/f/custom";
        }
    }

}
