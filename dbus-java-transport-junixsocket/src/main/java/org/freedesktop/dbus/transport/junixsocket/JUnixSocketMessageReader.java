package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.spi.message.AbstractInputStreamMessageReader;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import org.newsclub.net.unix.FileDescriptorCast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class JUnixSocketMessageReader extends AbstractInputStreamMessageReader {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public JUnixSocketMessageReader(AFUNIXSocketChannel _socket, boolean _hasFileDescriptorSupport) {
        super(_socket, _hasFileDescriptorSupport);
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
                        fds.add(new org.freedesktop.dbus.FileDescriptor(FileDescriptorCast.using(fd).as(Integer.class)));
                    }

                    logger.debug("=> {}", fds);
                    return fds;
                }
            } catch (IOException _ex) {
                throw new DBusException("Cannot read file descriptors", _ex);
            }
        }
        return null;
    }

}
