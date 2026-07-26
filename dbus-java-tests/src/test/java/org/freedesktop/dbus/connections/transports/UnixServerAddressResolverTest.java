package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Unit tests for {@link UnixServerAddressResolver} (A5): resolution of the listen-side unix address parameters
 * {@code dir}, {@code tmpdir} and {@code runtime} into a concrete {@code path}/{@code abstract}.
 */
class UnixServerAddressResolverTest extends AbstractBaseTest {

    private static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    void testDirResolvesToRandomPath() throws Exception {
        BusAddress address = BusAddress.of("unix:dir=" + TMP + ",listen=true");
        UnixServerAddressResolver.resolve(address, false);

        assertTrue(address.hasParameter("path"), "path should have been resolved");
        String path = address.getParameterValue("path");
        assertTrue(path.startsWith(new File(TMP, "dbus-").getAbsolutePath()), "unexpected path: " + path);
        assertFalse(address.hasParameter("abstract"), "no abstract expected for dir");
    }

    @Test
    void testTmpdirUsesAbstractWhenSupported() throws Exception {
        BusAddress address = BusAddress.of("unix:tmpdir=" + TMP + ",listen=true");
        UnixServerAddressResolver.resolve(address, true);

        assertTrue(address.hasParameter("abstract"), "abstract should have been resolved for tmpdir when supported");
        assertFalse(address.hasParameter("path"), "no path expected when abstract is used");
    }

    @Test
    void testTmpdirUsesPathWhenAbstractUnsupported() throws Exception {
        BusAddress address = BusAddress.of("unix:tmpdir=" + TMP + ",listen=true");
        UnixServerAddressResolver.resolve(address, false);

        assertTrue(address.hasParameter("path"), "path should have been resolved for tmpdir when abstract unsupported");
        assertFalse(address.hasParameter("abstract"), "no abstract expected when unsupported");
    }

    @Test
    void testRuntimeYes() throws Exception {
        BusAddress address = BusAddress.of("unix:runtime=yes,listen=true");
        String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");

        if (xdgRuntimeDir != null && !xdgRuntimeDir.isBlank()) {
            UnixServerAddressResolver.resolve(address, false);
            assertEquals(new File(xdgRuntimeDir, "bus").getAbsolutePath(), address.getParameterValue("path"));
        } else {
            // without XDG_RUNTIME_DIR the resolver must reject runtime=yes
            assertThrows(TransportConfigurationException.class, () -> UnixServerAddressResolver.resolve(address, false));
        }
    }

    @Test
    void testRuntimeRejectsInvalidValue() {
        BusAddress address = BusAddress.of("unix:runtime=nope,listen=true");
        assertThrows(TransportConfigurationException.class, () -> UnixServerAddressResolver.resolve(address, false));
    }

    @Test
    void testClientAddressIsNotResolved() throws Exception {
        // no listen=true -> client address; dir must be left untouched
        BusAddress address = BusAddress.of("unix:dir=" + TMP);
        UnixServerAddressResolver.resolve(address, false);

        assertFalse(address.hasParameter("path"), "client address must not be resolved");
        assertFalse(address.hasParameter("abstract"), "client address must not be resolved");
    }

    @Test
    void testExistingPathIsKept() throws Exception {
        BusAddress address = BusAddress.of("unix:path=/tmp/existing.sock,listen=true,dir=" + TMP);
        UnixServerAddressResolver.resolve(address, false);

        assertEquals("/tmp/existing.sock", address.getParameterValue("path"), "existing path must be kept");
    }
}
