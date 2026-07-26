package org.freedesktop.dbus.transport.tcp;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verifies the {@code nonce-tcp} transport: a client must present the 16-byte nonce read from the
 * {@code noncefile} before the D-Bus/SASL handshake, otherwise the connection is rejected.
 * <p>
 * These tests require the TCP transport on the classpath (only present in the TCP test run) and are disabled
 * otherwise.
 * </p>
 */
@EnabledIf("isNonceTcpAvailable")
class NonceTcpTransportTest extends AbstractBaseTest {

    @Test
    void testNonceTcpConnectSucceedsWithValidNonce() throws Exception {
        Path nonceFile = Files.createTempFile("dbus-nonce-", ".tmp");
        try {
            String base = nonceTcpBase(nonceFile);
            BusAddress connectAddress = BusAddress.of(base);
            BusAddress listenAddress = BusAddress.of(base + ",listen=true");

            try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
                daemon.startInBackgroundAndWait(MAX_WAIT);

                try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress).withShared(false).build()) {
                    assertNotNull(conn.getUniqueName(), "connection should be established with a valid nonce");
                }
            }
        } finally {
            Files.deleteIfExists(nonceFile);
        }
    }

    @Test
    void testNonceTcpConnectFailsWithWrongNonce() throws Exception {
        Path nonceFile = Files.createTempFile("dbus-nonce-", ".tmp");
        try {
            String base = nonceTcpBase(nonceFile);
            BusAddress connectAddress = BusAddress.of(base);
            BusAddress listenAddress = BusAddress.of(base + ",listen=true");

            try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
                daemon.startInBackgroundAndWait(MAX_WAIT);

                // corrupt the nonce file so the client sends a nonce that does not match the server's
                Files.write(nonceFile, new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});

                assertThrows(Exception.class, () -> {
                    // use a short timeout to keep connection retries (and thus the test) brief
                    try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress)
                            .transportConfig().withTimeout(1000).back()
                            .withShared(false).build()) {
                        fail("connection must be rejected when the nonce is invalid");
                    }
                });
            }
        } finally {
            Files.deleteIfExists(nonceFile);
        }
    }

    /**
     * Condition for {@link EnabledIf}: the nonce-tcp transport is only available when the TCP transport is on the
     * classpath (which is only the case in the TCP test run).
     */
    static boolean isNonceTcpAvailable() {
        return TransportBuilder.getRegisteredBusTypes().contains("NONCE-TCP");
    }

    /**
     * Builds a {@code nonce-tcp} base address (host/port/guid taken from a dynamic TCP session) pointing at the
     * given nonce file.
     */
    private static String nonceTcpBase(Path _nonceFile) {
        String tcp = TransportBuilder.createDynamicSession("TCP", false);
        return tcp.replaceFirst("^tcp:", "nonce-tcp:") + ",noncefile=" + _nonceFile;
    }
}
