package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.IFileBasedBusAddress;
import org.freedesktop.dbus.utils.Util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class UnixBusAddress extends BusAddress implements IFileBasedBusAddress {
    public UnixBusAddress(BusAddress _busAddress) {
        super(_busAddress);
    }

    public Path getPath() {
        return Paths.get(getParameterValue("path"));
    }

    public boolean hasPath() {
        return hasParameter("path");
    }

    public boolean isAbstract() {
        return hasParameter("abstract");
    }

    public String getAbstract() {
        return getParameterValue("abstract");
    }

    @Override
    public void updatePermissions(String _fileOwner, String _fileGroup, Set<PosixFilePermission> _fileUnixPermissions) {
        Util.setFilePermissions(getPath(), _fileOwner, _fileGroup, _fileUnixPermissions);
    }
}
