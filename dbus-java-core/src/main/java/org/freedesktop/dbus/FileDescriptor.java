package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.spi.filedescriptors.IFileDescriptorHelper;
import org.freedesktop.dbus.spi.filedescriptors.ReflectionFileDescriptorHelper;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Represents a FileDescriptor to be passed over the bus.  Can be created from
 * either an integer(gotten through some JNI/JNA/JNR call) or from a
 * java.io.FileDescriptor.
 *
 */
public class FileDescriptor {
    private static final ServiceLoader<IFileDescriptorHelper> HELPERS = ServiceLoader.load(IFileDescriptorHelper.class, FileDescriptor.class.getClassLoader());

    private final int fd;

    public FileDescriptor(int _fd) {
        fd = _fd;
    }

    public FileDescriptor(java.io.FileDescriptor _data) throws MarshallingException {
        fd = getFileDescriptor(_data);
    }

    public java.io.FileDescriptor toJavaFileDescriptor() throws MarshallingException {
        for (IFileDescriptorHelper helper : HELPERS) {
            Optional<java.io.FileDescriptor> result = helper.createFileDescriptor(fd);
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
        for (IFileDescriptorHelper helper : HELPERS) {
            Optional<Integer> result = helper.getFileDescriptorValue(_data);
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
