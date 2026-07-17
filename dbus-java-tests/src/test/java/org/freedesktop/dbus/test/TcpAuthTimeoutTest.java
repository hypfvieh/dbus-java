package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that the SASL handshake over TCP aborts within the configured timeout when the peer accepts the
 * connection but never sends any data (Slowloris-style), instead of blocking the handshake thread forever.
 */
public class TcpAuthTimeoutTest extends AbstractBaseTest {

    @Test
    void testTcpAuthTimesOutOnSilentPeer() throws Exception {
        // the tests module runs once per transport with the others excluded from the classpath; only run here
        // when the TCP transport is actually available
        Assumptions.assumeTrue(TransportBuilder.getRegisteredBusTypes().contains("TCP"),
            "TCP transport not on classpath in this execution");

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Thread accepter = new Thread(() -> {
                try (Socket accepted = server.accept()) {
                    // accept the connection but never send any SASL data
                    TimeUnit.SECONDS.sleep(30);
                } catch (Exception _ex) {
                    // ignore (socket closed / interrupted)
                }
            }, "silent-sasl-server");
            accepter.setDaemon(true);
            accepter.start();

            BusAddress address = BusAddress.of("tcp:host=127.0.0.1,port=" + server.getLocalPort());

            // With the watchdog the handshake must abort within ~timeout; without it, build() hangs forever.
            // timeout=500 -> single connect attempt (no retries), so the whole thing finishes quickly.
            assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(AuthenticationException.class, () -> {
                    try (AbstractTransport transport = TransportBuilder.create(address)
                            .configure().withTimeout(500).back().build()) {
                        fail("connect/authenticate must not succeed against a silent peer");
                    }
                }));
        }
    }
}
