package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * End-to-end test that a unix server can listen using the {@code dir} parameter (A5): the transport must resolve
 * {@code dir} into a concrete socket path, create the socket there, and accept a client connecting to that path.
 * <p>
 * Requires one of the unix transports on the classpath; disabled in the TCP run.
 * </p>
 */
@EnabledIf("isUnixAvailable")
class UnixDirListenTest extends AbstractBaseTest {

    static boolean isUnixAvailable() {
        return TransportBuilder.getRegisteredBusTypes().contains("UNIX");
    }

    @Test
    void testListenUsingDirParameter() throws Exception {
        Path dir = Files.createTempDirectory("dbus-dir-test-");
        try {
            BusAddress listenAddress = BusAddress.of("unix:dir=" + dir + ",listen=true");

            try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenAddress)) {
                daemon.startInBackgroundAndWait(MAX_WAIT);

                // the transport must have created a "dbus-..." socket inside the directory
                Path socket = findSocket(dir);
                assertNotNull(socket, "server should have created a socket inside the dir");

                BusAddress connectAddress = BusAddress.of("unix:path=" + socket);
                try (DBusConnection conn = DBusConnectionBuilder.forAddress(connectAddress).withShared(false).build()) {
                    assertNotNull(conn.getUniqueName(), "client should connect to the dir-based socket");
                }
            }
        } finally {
            deleteRecursively(dir);
        }
    }

    private static Path findSocket(Path _dir) throws IOException {
        try (Stream<Path> files = Files.list(_dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith("dbus-")).findFirst().orElse(null);
        }
    }

    private static void deleteRecursively(Path _dir) throws IOException {
        try (Stream<Path> walk = Files.walk(_dir)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }
}
