package sample.issue;

import org.freedesktop.dbus.RemoteInvocationHandler;
import org.freedesktop.dbus.RemoteObject;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

public class Issue196Test extends AbstractBaseTest {

    @Test
    public void testCorrectInterfaceCreation() throws Exception {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        BusAddress busAddress = TransportBuilder.createWithDynamicSession(protocolType).configure().build()
            .getBusAddress();

        BusAddress listenBusAddress = BusAddress.of(busAddress).getListenerAddress();

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);
            logger.debug("Started embedded bus on address {}", listenBusAddress);

            // connect to started daemon process
            logger.info("Connecting to embedded DBus {}", busAddress);

            final String source = "dbus.issue196";
            final String path = "/dbus/issue196";

            try (DBusConnection connection = DBusConnectionBuilder.forAddress(busAddress).build()) {
                DBusInterface dbi = null;
                RemoteInvocationHandler rih = null;

                connection.requestBusName(source);
                connection.exportObject(path, (TestInterfaceType) () -> path);

                dbi = connection.dynamicProxy(source, path, null);
                assertNotNull(dbi);

                rih = RemoteInvocationHandler.class.cast(Proxy.getInvocationHandler(dbi));
                assertNotNull(rih);

                RemoteObject ro = rih.getRemote();

                assertNull(ro.getInterface());

                Class<? extends DBusInterface> type = TestInterfaceType.class;

                dbi = connection.dynamicProxy(source, path, type);
                assertNotNull(dbi);
                assertTrue(TestInterfaceType.class.isInstance(dbi));

                rih = RemoteInvocationHandler.class.cast(Proxy.getInvocationHandler(dbi));
                assertNotNull(rih);

                // RemoteInvocationHandler.remote is package scope
                ro = rih.getRemote();

                assertNotNull(ro.getInterface());
                assertSame(type, ro.getInterface());
            }
        }
    }

    @DBusInterfaceName("dbus.issue196.TestInterfaceType")
    public interface TestInterfaceType extends DBusInterface {

    }

}
