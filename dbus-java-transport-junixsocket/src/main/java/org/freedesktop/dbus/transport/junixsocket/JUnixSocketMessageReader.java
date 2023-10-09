package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.spi.message.AbstractInputStreamMessageReader;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.newsclub.net.unix.AFUNIXSocketChannel;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JUnixSocketMessageReader extends AbstractInputStreamMessageReader {

    public JUnixSocketMessageReader(AFUNIXSocketChannel _socket, ISocketProvider _socketProviderImpl) {
        super(_socket, _socketProviderImpl);
    }

    @Override
    protected List<org.freedesktop.dbus.FileDescriptor> readFileDescriptors(SocketChannel _inputChannel) throws DBusException {
        if (_inputChannel instanceof AFUNIXSocketChannel) {
            try {
                FileDescriptor[] receivedFileDescriptors = ((AFUNIXSocketChannel) _inputChannel).getReceivedFileDescriptors();
                if (receivedFileDescriptors.length == 0) {
                    return null;
                } else {
                    List<org.freedesktop.dbus.FileDescriptor> fds = new ArrayList<>();
                    for (FileDescriptor fd : receivedFileDescriptors) {
                        Optional<Integer> fileDescriptorValue = getSocketProviderImpl().getFileDescriptorValue(fd);
                        fileDescriptorValue.ifPresent(f -> fds.add(new org.freedesktop.dbus.FileDescriptor(f)));
                    }

                    getLogger().debug("=> {}", fds);
                    return fds;
                }
            } catch (IOException _ex) {
                throw new DBusException("Cannot read file descriptors", _ex);
            }
        }
        return null;
    }

}
