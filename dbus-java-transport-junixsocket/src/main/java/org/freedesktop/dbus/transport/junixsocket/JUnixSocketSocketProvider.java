package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.spi.message.*;
import org.newsclub.net.unix.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class JUnixSocketSocketProvider implements ISocketProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(JUnixSocketSocketProvider.class);

    private boolean             hasFileDescriptorSupport = true;

    @Override
    public IMessageReader createReader(SocketChannel _socket) {
        if (!AFSocket.supports(AFSocketCapability.CAPABILITY_UNIX_DOMAIN)) {
            return null;
        }
        if (_socket instanceof AFUNIXSocketChannel) {
            return new JUnixSocketMessageReader((AFUNIXSocketChannel) _socket, this);
        }
        return null;
    }

    @Override
    public IMessageWriter createWriter(SocketChannel _socket) {
        if (!AFSocket.supports(AFSocketCapability.CAPABILITY_UNIX_DOMAIN)) {
            return null;
        }
        if (_socket instanceof AFUNIXSocketChannel) {
            return new JUnixSocketMessageWriter((AFUNIXSocketChannel) _socket, this);
        }
        return null;
    }

    @Override
    public void setFileDescriptorSupport(boolean _support) {
        hasFileDescriptorSupport = _support;
    }

    @Override
    public boolean isFileDescriptorPassingSupported() {
        return hasFileDescriptorSupport && AFSocket.supports(AFSocketCapability.CAPABILITY_FILE_DESCRIPTORS) && AFSocket.supports(AFSocketCapability.CAPABILITY_UNSAFE);
    }

    @Override
    public Optional<Integer> getFileDescriptorValue(FileDescriptor _fd) {
        try {
            return Optional.of(FileDescriptorCast.using(_fd).as(Integer.class));
        } catch (IOException | ClassCastException _ex) {
            LOGGER.error("Could not get file descriptor by using junixsocket library", _ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileDescriptor> createFileDescriptor(int _fd) {
        if (!AFSocket.supports(AFSocketCapability.CAPABILITY_UNSAFE)) {
            LOGGER.debug("Could not create new FileDescriptor instance by using junixsocket library, as unsafe capabilities of that library is disabled.");
            return Optional.empty();
        }
        try {
            return Optional.of(FileDescriptorCast.unsafeUsing(_fd).getFileDescriptor());
        } catch (IOException _ex) {
            LOGGER.error("Could not create new FileDescriptor instance by using junixsocket library.", _ex);
            return Optional.empty();
        }
    }
}
