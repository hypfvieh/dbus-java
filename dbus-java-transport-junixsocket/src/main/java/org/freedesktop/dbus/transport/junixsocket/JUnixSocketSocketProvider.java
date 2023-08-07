package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.spi.message.IMessageReader;
import org.freedesktop.dbus.spi.message.IMessageWriter;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.newsclub.net.unix.AFSocket;
import org.newsclub.net.unix.AFSocketCapability;
import org.newsclub.net.unix.AFUNIXSocketChannel;

import java.nio.channels.SocketChannel;

public class JUnixSocketSocketProvider implements ISocketProvider {
    private boolean hasFileDescriptorSupport = false;

    @Override
    public IMessageReader createReader(SocketChannel _socket) {
        if (!AFSocket.supports(AFSocketCapability.CAPABILITY_UNIX_DOMAIN)) {
            return null;
        }
        if (_socket instanceof AFUNIXSocketChannel) {
            return new JUnixSocketMessageReader((AFUNIXSocketChannel) _socket, hasFileDescriptorSupport);
        }
        return null;
    }

    @Override
    public IMessageWriter createWriter(SocketChannel _socket) {
        if (!AFSocket.supports(AFSocketCapability.CAPABILITY_UNIX_DOMAIN)) {
            return null;
        }
        if (_socket instanceof AFUNIXSocketChannel) {
            return new JUnixSocketMessageWriter((AFUNIXSocketChannel) _socket, hasFileDescriptorSupport);
        }
        return null;
    }

    @Override
    public void setFileDescriptorSupport(boolean _support) {
        hasFileDescriptorSupport = _support;
    }

    @Override
    public boolean isFileDescriptorPassingSupported() {
        return AFSocket.supports(AFSocketCapability.CAPABILITY_FILE_DESCRIPTORS) && AFSocket.supports(AFSocketCapability.CAPABILITY_UNSAFE);
    }
}
