package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.spi.filedescriptors.IFileDescriptorHelper;
import org.newsclub.net.unix.FileDescriptorCast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Optional;

public class JUnixSocketFileDescriptorHelper implements IFileDescriptorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JUnixSocketFileDescriptorHelper.class);

    @Override
    public Optional<Integer> getFileDescriptorValue(FileDescriptor _fd) {
        try {
            return Optional.of(FileDescriptorCast.using(_fd).as(Integer.class));
        } catch (IOException _ex) {
            LOGGER.error("Could not get file descriptor by using junixsocket library", _ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileDescriptor> createFileDescriptor(int _fd) {
        try {
            return Optional.of(FileDescriptorCast.unsafeUsing(_fd).getFileDescriptor());
        } catch (IOException _ex) {
            LOGGER.error("Could not create new FileDescriptor instance by using junixsocket library.", _ex);
            return Optional.empty();
        }
    }
}
