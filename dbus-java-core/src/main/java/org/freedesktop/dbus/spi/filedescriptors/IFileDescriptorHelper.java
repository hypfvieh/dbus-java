package org.freedesktop.dbus.spi.filedescriptors;

import java.io.FileDescriptor;
import java.util.Optional;

/**
 * Interface used by {@link java.util.ServiceLoader ServiceLoader} to provide a helper to work with file descriptors.
 */
public interface IFileDescriptorHelper {
    Optional<Integer> getFileDescriptorValue(FileDescriptor _fd);

    Optional<FileDescriptor> createFileDescriptor(int _fd);
}
