package org.freedesktop.dbus.connections.transports;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.exceptions.TransportConfigurationException;
import org.freedesktop.dbus.utils.Util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Set;

/**
 * Common base class for the unix socket {@link BusAddress} variants of the different transports.
 * <p>
 * Besides the shared {@code path}/{@code abstract} accessors it resolves the listen-side address parameters
 * {@code dir}, {@code tmpdir} and {@code runtime} into a concrete {@code path} (or {@code abstract}) as described by
 * the D-Bus specification. This resolution happens once, when the (transport-internal) address copy is created, so the
 * {@link BusAddress} provided by the caller is never modified.
 * </p>
 *
 * @since 6.0.0
 */
public abstract class AbstractUnixBusAddress extends BusAddress implements IFileBasedBusAddress {

    private static final SecureRandom RANDOM       = new SecureRandom();
    private static final int          RANDOM_CHARS = 10;

    private static final String       PATH         = "path";
    private static final String       ABSTRACT     = "abstract";

    /**
     * Creates a new unix bus address from the given (already parsed) address and resolves the listen-side
     * {@code dir}/{@code tmpdir}/{@code runtime} parameters.
     *
     * @param _obj source address (copied, never modified)
     * @param _supportsAbstract whether the concrete transport supports abstract sockets (relevant for {@code tmpdir})
     *
     * @throws TransportConfigurationException if a parameter value is invalid or the environment is incomplete
     */
    protected AbstractUnixBusAddress(BusAddress _obj, boolean _supportsAbstract) throws TransportConfigurationException {
        super(_obj);
        resolveServerAddress(_supportsAbstract);
    }

    public boolean hasPath() {
        return hasParameter(PATH);
    }

    public String getPath() {
        return getParameterValue(PATH);
    }

    public boolean isAbstract() {
        return hasParameter(ABSTRACT);
    }

    public String getAbstract() {
        return getParameterValue(ABSTRACT);
    }

    @Override
    public void updatePermissions(String _fileOwner, String _fileGroup, Set<PosixFilePermission> _fileUnixPermissions) {
        Util.setFilePermissions(Path.of(getPath()), _fileOwner, _fileGroup, _fileUnixPermissions);
    }

    /**
     * Resolves the listen-side {@code dir}/{@code tmpdir}/{@code runtime} parameters into a concrete {@code path} or
     * {@code abstract} parameter. Only applies to listening addresses that do not already carry a concrete socket.
     */
    private void resolveServerAddress(boolean _supportsAbstract) throws TransportConfigurationException {
        if (!isListeningSocket() || hasParameter(PATH) || hasParameter(ABSTRACT)) {
            return;
        }

        if (hasParameter("runtime")) {
            String runtime = getParameterValue("runtime");
            if (!"yes".equals(runtime)) {
                throw new TransportConfigurationException("unix address parameter 'runtime' only accepts the value 'yes'");
            }
            String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
            if (xdgRuntimeDir == null || xdgRuntimeDir.isBlank()) {
                throw new TransportConfigurationException("runtime=yes requires the XDG_RUNTIME_DIR environment variable to be set");
            }
            addParameter(PATH, new File(xdgRuntimeDir, "bus").getAbsolutePath());
        } else if (hasParameter("dir")) {
            addParameter(PATH, randomSocketPath(getParameterValue("dir")));
        } else if (hasParameter("tmpdir")) {
            String tmpDir = getParameterValue("tmpdir");
            if (_supportsAbstract) {
                addParameter(ABSTRACT, randomSocketPath(tmpDir));
            } else {
                addParameter(PATH, randomSocketPath(tmpDir));
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
