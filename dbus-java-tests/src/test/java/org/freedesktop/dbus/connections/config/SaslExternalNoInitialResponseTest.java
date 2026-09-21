package org.freedesktop.dbus.connections.config;

import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.SASL.SaslMode;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that a SASL server accepts an EXTERNAL authentication attempt where the client omits
 * the initial identity, as done by clients such as godbus since
 * https://github.com/godbus/dbus/commit/31b5df72caaf5c68ec5ff414944e8ab8c24f8c52 (see
 * https://github.com/hypfvieh/dbus-java/issues/294). Prior to the fix, the server rejected such a
 * bare {@code AUTH EXTERNAL} outright instead of following the D-Bus-spec-mandated DATA
 * round-trip, which made the peer abort the connection and the server's next read fail with a
 * {@code SocketClosedException}. Lives in the {@code ...config} package to access the
 * package-private {@link SaslConfig} constructor; only the public {@link SASL} API is exercised.
 */
class SaslExternalNoInitialResponseTest extends AbstractBaseTest {

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testServerAcceptsExternalWithoutInitialResponse() throws Exception {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

            SaslConfig serverCfg = new SaslConfig();
            serverCfg.setMode(SaslMode.SERVER);
            serverCfg.setAuthMode(SASL.AUTH_EXTERNAL);
            serverCfg.setGuid("00000000000000000000000000000000");

            ExecutorService exec = Executors.newSingleThreadExecutor();
            try {
                Future<Boolean> serverFuture = exec.submit(() -> {
                    try (SocketChannel serverCh = ssc.accept()) {
                        return new SASL(serverCfg).auth(serverCh, null);
                    }
                });

                try (SocketChannel clientCh = SocketChannel.open(ssc.getLocalAddress())) {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(Channels.newInputStream(clientCh), StandardCharsets.US_ASCII));

                    // mimic a client that authenticates without sending an explicit identity
                    // along with "AUTH EXTERNAL", relying purely on the transport's credentials
                    clientCh.write(ByteBuffer.wrap(new byte[] {0}));
                    write(clientCh, "AUTH EXTERNAL\r\n");

                    assertEquals("DATA", in.readLine(),
                        "server must poke for the identity instead of rejecting immediately");

                    write(clientCh, "DATA\r\n");

                    String reply = in.readLine();
                    assertTrue(reply != null && reply.startsWith("OK "),
                        "server must accept the identity established via the transport, got: " + reply);

                    write(clientCh, "BEGIN\r\n");
                }

                assertTrue(serverFuture.get(10, TimeUnit.SECONDS), "server auth should succeed");
            } finally {
                exec.shutdown();
            }
        }
    }

    private static void write(SocketChannel _ch, String _s) throws IOException {
        _ch.write(ByteBuffer.wrap(_s.getBytes(StandardCharsets.US_ASCII)));
    }
}
