package org.freedesktop.dbus.connections.config;

import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.connections.SASL.SaslMode;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that a SASL client which requested unix file descriptor support still completes the
 * authentication (without fd support) when the server rejects the {@code NEGOTIATE_UNIX_FD} request
 * with {@code ERROR}. Lives in the {@code ...config} package to access the package-private
 * {@link SaslConfig} constructor; only the public {@link SASL} API is exercised.
 */
class SaslFileDescriptorNegotiationTest extends AbstractBaseTest {

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testClientContinuesWhenServerRejectsUnixFd() throws Exception {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

            SaslConfig serverCfg = new SaslConfig();
            serverCfg.setMode(SaslMode.SERVER);
            serverCfg.setAuthMode(SASL.AUTH_ANON);
            serverCfg.setGuid("00000000000000000000000000000000");
            serverCfg.setFileDescriptorSupport(false); // server rejects NEGOTIATE_UNIX_FD with ERROR

            SaslConfig clientCfg = new SaslConfig();
            clientCfg.setMode(SaslMode.CLIENT);
            clientCfg.setAuthMode(SASL.AUTH_ANON);
            clientCfg.setFileDescriptorSupport(true); // client requests fd support

            try (ExecutorService exec = Executors.newSingleThreadExecutor()) {
                Future<Boolean> serverFuture = exec.submit(() -> {
                    try (SocketChannel serverCh = ssc.accept()) {
                        return new SASL(serverCfg).auth(serverCh, null);
                    }
                });

                SASL clientSasl = new SASL(clientCfg);
                try (SocketChannel clientCh = SocketChannel.open(ssc.getLocalAddress())) {
                    boolean clientResult = clientSasl.auth(clientCh, null);
                    assertTrue(clientResult,
                        "client auth should succeed even when the server rejects unix fd support");
                    assertFalse(clientSasl.isFileDescriptorSupported(),
                        "fd support must be disabled after the server replied ERROR to NEGOTIATE_UNIX_FD");
                    assertTrue(serverFuture.get(10, TimeUnit.SECONDS), "server auth should succeed");
                }
            }
        }
    }
}
