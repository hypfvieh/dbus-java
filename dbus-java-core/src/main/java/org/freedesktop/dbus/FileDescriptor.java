package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.freedesktop.dbus.utils.ReflectionFileDescriptorHelper;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Represents a FileDescriptor to be passed over the bus.  Can be created from
 * either an integer(gotten through some JNI/JNA/JNR call) or from a
 * java.io.FileDescriptor.
 */
public final class FileDescriptor {
    private static final ServiceLoader<ISocketProvider> SPI_LOADER = ServiceLoader.load(ISocketProvider.class, FileDescriptor.class.getClassLoader());

    private final int fd;

    public FileDescriptor(int _fd) {
        fd = _fd;
    }

    public FileDescriptor(java.io.FileDescriptor _data) throws MarshallingException {
        fd = getFileDescriptor(_data);
    }

    public java.io.FileDescriptor toJavaFileDescriptor() throws MarshallingException {
        for (ISocketProvider provider : SPI_LOADER) {
            Optional<java.io.FileDescriptor> result = provider.createFileDescriptor(fd);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return ReflectionFileDescriptorHelper.getInstance()
                .flatMap(helper -> helper.createFileDescriptor(fd))
                .orElseThrow(() -> new MarshallingException("Could not create new FileDescriptor instance"));
    }

    public int getIntFileDescriptor() {
        return fd;
    }

    private int getFileDescriptor(java.io.FileDescriptor _data) throws MarshallingException {
        for (ISocketProvider provider : SPI_LOADER) {
            Optional<Integer> result = provider.getFileDescriptorValue(_data);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return ReflectionFileDescriptorHelper.getInstance()
                .flatMap(helper -> helper.getFileDescriptorValue(_data))
                .orElseThrow(() -> new MarshallingException("Could not get FileDescriptor value"));
    }

    @Override
    public boolean equals(Object _o) {
        if (this == _o) {
            return true;
        }
        if (_o == null || getClass() != _o.getClass()) {
            return false;
        }
        FileDescriptor that = (FileDescriptor) _o;
        return fd == that.fd;
    }

    @Override
    public int hashCode() {
        return fd;
    }

    @Override
    public String toString() {
        return FileDescriptor.class.getSimpleName() + "[fd=" + fd + "]";
    }
}
