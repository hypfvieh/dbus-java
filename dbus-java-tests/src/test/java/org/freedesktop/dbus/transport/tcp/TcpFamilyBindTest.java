package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.utils.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Verifies the TCP transport address parameters {@code bind} and {@code family}.
 * <p>
 * These tests require the TCP transport on the classpath (only present in the TCP test run) and are disabled
 * otherwise.
 * </p>
 */
@EnabledIf("isTcpAvailable")
class TcpFamilyBindTest extends AbstractBaseTest {

    static boolean isTcpAvailable() {
        return TransportBuilder.getRegisteredBusTypes().contains("TCP");
    }

    @Test
    void testBindAddressIsUsedInsteadOfHost() throws Exception {
        int port = freePort();
        String guid = Util.genGUID();

        // advertised host is 127.0.0.1, but the server must actually bind to 127.0.0.2 (both are loopback on Linux).
        // Without honouring 'bind' the server would listen on 127.0.0.1 and the connect below would be refused.
        BusAddress listenAddress = BusAddress.of("tcp:host=127.0.0.1,port=" + port + ",guid=" + guid + ",bind=127.0.0.2,listen=true");
        BusAddress connectAddress = BusAddress.of("tcp:host=127.0.0.2,port=" + port + ",guid=" + guid);

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress).withShared(false).build()) {
                assertNotNull(conn.getUniqueName(), "server must bind to the 'bind' address, not the advertised host");
            }
        }
    }

    @Test
    void testBindToAllInterfaces() throws Exception {
        String base = TransportBuilder.createDynamicSession("TCP", false);
        BusAddress listenAddress = BusAddress.of(base + ",bind=*,listen=true");
        BusAddress connectAddress = BusAddress.of(base);

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress).withShared(false).build()) {
                assertNotNull(conn.getUniqueName(), "connection should succeed against a wildcard-bound server");
            }
        }
    }

    @Test
    void testExplicitIpv4Family() throws Exception {
        String base = TransportBuilder.createDynamicSession("TCP", false);
        BusAddress listenAddress = BusAddress.of(base + ",family=ipv4,listen=true");
        BusAddress connectAddress = BusAddress.of(base + ",family=ipv4");

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress).withShared(false).build()) {
                assertNotNull(conn.getUniqueName(), "connection should succeed when forcing IPv4");
            }
        }
    }

    @Test
    void testUnsupportedFamilyFails() throws Exception {
        // start a working server, then attempt to connect to the very same address but with an invalid family.
        // A running server ensures the connection would succeed if the family were ignored, so the expected failure
        // can only come from the family validation itself.
        String base = TransportBuilder.createDynamicSession("TCP", false);
        BusAddress listenAddress = BusAddress.of(base + ",listen=true");
        BusAddress connectAddress = BusAddress.of(base + ",family=bogus");

        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
            daemon.startInBackgroundAndWait(MAX_WAIT);

            assertThrows(Exception.class, () -> {
                try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress)
                        .transportConfig().withTimeout(1000).back()
                        .withShared(false).build()) {
                    fail("connection must fail for an unsupported address family");
                }
            });
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket()) {
            s.bind(null);
            return s.getLocalPort();
        }
    }
}
