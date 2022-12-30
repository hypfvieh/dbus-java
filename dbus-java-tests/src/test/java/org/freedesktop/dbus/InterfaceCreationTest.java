// The package must be "both" org.freedesktop.dbus.connections.impl and org.freedesktop.dbus in order
// for the test to be able to access the requisite methods and fields... obviously this is an issue...
package org.freedesktop.dbus;

import java.lang.reflect.Proxy;

import org.freedesktop.DBus;
import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InterfaceCreationTest extends AbstractBaseTest {

	@Test
	public void testCorrectInterfaceCreation() throws Exception {
		String protocolType = TransportBuilder.getRegisteredBusTypes().get(0);
		BusAddress busAddress = TransportBuilder.createWithDynamicSession(protocolType).configure().build()
			.getBusAddress();

		BusAddress listenBusAddress = BusAddress.of(busAddress).getListenerAddress();

		try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
			daemon.startInBackground();
			this.logger.debug("Started embedded bus on address {}", listenBusAddress);

			waitForDaemon(daemon);

			// connect to started daemon process
			this.logger.info("Connecting to embedded DBus {}", busAddress);

			final String source = "org.freedesktop.DBus";
			final String path = "/org/freedesktop/DBus";

			try (DBusConnection connection = DBusConnectionBuilder.forAddress(busAddress).build()) {
				DBusInterface dbi = null;
				RemoteInvocationHandler rih = null;
				RemoteObject ro = null;
				Class<? extends DBusInterface> type = DBus.Peer.class;

				dbi = connection.getExportedObject(source, path, null);
				Assertions.assertNotNull(dbi);

				rih = RemoteInvocationHandler.class.cast(Proxy.getInvocationHandler(dbi));
				Assertions.assertNotNull(rih);

				ro = rih.remote;

				Assertions.assertNull(ro.getInterface());

				dbi = connection.getExportedObject(source, path, type);
				Assertions.assertNotNull(dbi);
				Assertions.assertTrue(type.isInstance(dbi));

				rih = RemoteInvocationHandler.class.cast(Proxy.getInvocationHandler(dbi));
				Assertions.assertNotNull(rih);

				ro = rih.remote;

				Assertions.assertNotNull(ro.getInterface());
				Assertions.assertSame(type, ro.getInterface());
			}
		}
	}

}
