package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.freedesktop.dbus.utils.ReflectionFileDescriptorHelper;

import java.util.Optional;

/**
 * Represents a FileDescriptor to be passed over the bus. <br>
 * Can be created from either an integer (gotten through some JNI/JNA/JNR call) or from a
 * {@link java.io.FileDescriptor}.
 */
public record FileDescriptor(int intFileDescriptor) {
    /**
     * Converts this DBus {@link FileDescriptor} to a {@link java.io.FileDescriptor}.<br>
     * Tries to use the provided ISocketProvider if present first. <br>
     * If not present or conversion failed, tries to convert using reflection.
     *
     * @param _provider provider or null
     *
     * @return java file descriptor
     * @throws MarshallingException when converting fails
     */
    public java.io.FileDescriptor toJavaFileDescriptor(ISocketProvider _provider) throws MarshallingException {
        if (_provider != null) {
            Optional<java.io.FileDescriptor> result = _provider.createFileDescriptor(intFileDescriptor);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return ReflectionFileDescriptorHelper.getInstance()
                .flatMap(helper -> helper.createFileDescriptor(intFileDescriptor))
                .orElseThrow(() -> new MarshallingException("Could not create new FileDescriptor instance"));
    }

    /**
     * Utility method to create a DBus {@link FileDescriptor} from a {@link java.io.FileDescriptor}.<br>
     * Tries to use the provided ISocketProvider if present first.<br>
     * If not present or conversion failed, tries to convert using reflection.
     *
     * @param _data file descriptor
     * @param _provider socket provider or null
     *
     * @return DBus FileDescriptor
     * @throws MarshallingException when conversion fails
     */
    public static FileDescriptor fromJavaFileDescriptor(java.io.FileDescriptor _data, ISocketProvider _provider) throws MarshallingException {
        if (_provider != null) {
            Optional<Integer> result = _provider.getFileDescriptorValue(_data);
            if (result.isPresent()) {
                return new FileDescriptor(result.get());
            }
        }

        return new FileDescriptor(ReflectionFileDescriptorHelper.getInstance()
            .flatMap(helper -> helper.getFileDescriptorValue(_data))
            .orElseThrow(() -> new MarshallingException("Could not get FileDescriptor value")));
    }
}
