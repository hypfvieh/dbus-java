package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Tests the listen-side unix address resolution ({@code dir}/{@code tmpdir}/{@code runtime} -&gt;
 * {@code path}/{@code abstract}) implemented in {@link AbstractUnixBusAddress}.
 */
class AbstractUnixBusAddressTest extends AbstractBaseTest {

    private static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    void testDirResolvesToRandomPath() throws Exception {
        TestUnixBusAddress address = new TestUnixBusAddress(BusAddress.of("unix:dir=" + TMP + ",listen=true"), false);

        assertTrue(address.hasPath(), "path should have been resolved");
        assertTrue(address.getPath().startsWith(new File(TMP, "dbus-").getAbsolutePath()), "unexpected path: " + address.getPath());
        assertFalse(address.isAbstract(), "no abstract expected for dir");
    }

    @Test
    void testTmpdirUsesAbstractWhenSupported() throws Exception {
        TestUnixBusAddress address = new TestUnixBusAddress(BusAddress.of("unix:tmpdir=" + TMP + ",listen=true"), true);

        assertTrue(address.isAbstract(), "abstract should have been resolved for tmpdir when supported");
        assertFalse(address.hasPath(), "no path expected when abstract is used");
    }

    @Test
    void testTmpdirUsesPathWhenAbstractUnsupported() throws Exception {
        TestUnixBusAddress address = new TestUnixBusAddress(BusAddress.of("unix:tmpdir=" + TMP + ",listen=true"), false);

        assertTrue(address.hasPath(), "path should have been resolved for tmpdir when abstract unsupported");
        assertFalse(address.isAbstract(), "no abstract expected when unsupported");
    }

    @Test
    void testRuntimeYes() throws Exception {
        BusAddress source = BusAddress.of("unix:runtime=yes,listen=true");
        String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");

        if (xdgRuntimeDir != null && !xdgRuntimeDir.isBlank()) {
            TestUnixBusAddress address = new TestUnixBusAddress(source, false);
            assertEquals(new File(xdgRuntimeDir, "bus").getAbsolutePath(), address.getPath());
        } else {
            // without XDG_RUNTIME_DIR the resolver must reject runtime=yes
            assertThrows(TransportConfigurationException.class, () -> new TestUnixBusAddress(source, false));
        }
    }

    @Test
    void testRuntimeRejectsInvalidValue() {
        BusAddress source = BusAddress.of("unix:runtime=nope,listen=true");
        assertThrows(TransportConfigurationException.class, () -> new TestUnixBusAddress(source, false));
    }

    @Test
    void testClientAddressIsNotResolved() throws Exception {
        // no listen=true -> client address; dir must be left untouched
        TestUnixBusAddress address = new TestUnixBusAddress(BusAddress.of("unix:dir=" + TMP), false);

        assertFalse(address.hasPath(), "client address must not be resolved");
        assertFalse(address.isAbstract(), "client address must not be resolved");
    }

    @Test
    void testExistingPathIsKept() throws Exception {
        TestUnixBusAddress address = new TestUnixBusAddress(BusAddress.of("unix:path=/tmp/existing.sock,listen=true,dir=" + TMP), false);

        assertEquals("/tmp/existing.sock", address.getPath(), "existing path must be kept");
    }

    @Test
    void testSourceAddressIsNotModified() throws Exception {
        // resolution happens on the copy: the caller-provided address must remain untouched
        BusAddress source = BusAddress.of("unix:dir=" + TMP + ",listen=true");
        new TestUnixBusAddress(source, false);

        assertFalse(source.hasParameter("path"), "source address must not be modified by resolution");
    }

    /** Minimal concrete {@link AbstractUnixBusAddress} to exercise the shared resolution logic. */
    private static final class TestUnixBusAddress extends AbstractUnixBusAddress {
        TestUnixBusAddress(BusAddress _obj, boolean _supportsAbstract) throws TransportConfigurationException {
            super(_obj, _supportsAbstract);
        }
    }
}
