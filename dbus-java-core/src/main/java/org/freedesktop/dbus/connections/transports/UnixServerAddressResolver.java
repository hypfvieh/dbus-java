package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;

import java.io.File;
import java.security.SecureRandom;

/**
 * Resolves the listen-side unix address parameters {@code dir}, {@code tmpdir} and {@code runtime} into a concrete
 * {@code path} (or {@code abstract}) as described by the D-Bus specification.
 * <p>
 * These parameters may only be used in server (listening) addresses; the resulting client address will contain the
 * concrete {@code path}/{@code abstract} instead. For client addresses (or addresses that already provide a concrete
 * {@code path}/{@code abstract}) this resolver does nothing.
 * </p>
 *
 * @since 6.0.0
 */
public final class UnixServerAddressResolver {

    private static final SecureRandom RANDOM       = new SecureRandom();
    private static final int          RANDOM_CHARS = 10;

    private UnixServerAddressResolver() {
    }

    /**
     * Resolves {@code dir}/{@code tmpdir}/{@code runtime} on the given listening unix address into a concrete
     * {@code path} or {@code abstract} parameter (added to the address in place).
     *
     * @param _address unix bus address
     * @param _supportsAbstract whether the transport supports abstract sockets (used for {@code tmpdir})
     *
     * @throws TransportConfigurationException if a parameter value is invalid or the environment is incomplete
     */
    public static void resolve(BusAddress _address, boolean _supportsAbstract) throws TransportConfigurationException {
        // only listening addresses carry dir/tmpdir/runtime, and only when no concrete socket was given
        if (!_address.isListeningSocket() || _address.hasParameter("path") || _address.hasParameter("abstract")) {
            return;
        }

        if (_address.hasParameter("runtime")) {
            String runtime = _address.getParameterValue("runtime");
            if (!"yes".equals(runtime)) {
                throw new TransportConfigurationException("unix address parameter 'runtime' only accepts the value 'yes'");
            }
            String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
            if (xdgRuntimeDir == null || xdgRuntimeDir.isBlank()) {
                throw new TransportConfigurationException("runtime=yes requires the XDG_RUNTIME_DIR environment variable to be set");
            }
            _address.addParameter("path", new File(xdgRuntimeDir, "bus").getAbsolutePath());
        } else if (_address.hasParameter("dir")) {
            _address.addParameter("path", randomSocketPath(_address.getParameterValue("dir")));
        } else if (_address.hasParameter("tmpdir")) {
            String tmpDir = _address.getParameterValue("tmpdir");
            if (_supportsAbstract) {
                _address.addParameter("abstract", randomSocketPath(tmpDir));
            } else {
                _address.addParameter("path", randomSocketPath(tmpDir));
            }
        }
        // no dir/tmpdir/runtime present: leave as-is, the transport will report the missing path/abstract
    }

    /**
     * Creates a not-yet-existing socket path with a random {@code dbus-XXXXXXXXXX} file name in the given directory.
     */
    private static String randomSocketPath(String _dir) {
        File file;
        do {
            StringBuilder sb = new StringBuilder("dbus-");
            for (int i = 0; i < RANDOM_CHARS; i++) {
                sb.append((char) (RANDOM.nextInt(26) + 'A'));
            }
            file = new File(_dir, sb.toString());
        } while (file.exists());
        return file.getAbsolutePath();
    }
}
