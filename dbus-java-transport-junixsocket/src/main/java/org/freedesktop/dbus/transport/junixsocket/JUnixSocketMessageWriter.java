package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.spi.message.IMessageWriter;
import org.freedesktop.dbus.utils.Hexdump;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import org.newsclub.net.unix.FileDescriptorCast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class JUnixSocketMessageWriter implements IMessageWriter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AFUNIXSocketChannel socket;
    private final boolean hasFileDescriptorSupport;
    private ByteBuffer buffer;

    public JUnixSocketMessageWriter(AFUNIXSocketChannel _socket, boolean _hasFileDescriptorSupport) {
        socket = _socket;
        hasFileDescriptorSupport = _hasFileDescriptorSupport;
    }

    @Override
    public void close() throws IOException {
        if (socket.isOpen()) {
            logger.debug("Closing Message Writer");
            socket.close();
        }
    }

    @Override
    public boolean isClosed() {
        return !socket.isOpen();
    }

    @Override
    public void writeMessage(Message _msg) throws IOException {
        logger.debug("<= {}", _msg);

        if (_msg == null) {
            return;
        }

        byte[][] wireData = _msg.getWireData();

        if (wireData == null) {
            logger.warn("Message {} wire-data was null!", _msg);
            return;
        }

        int totalSize = 0;

        for (byte[] bytes : wireData) {
            if (bytes == null) {
                break;
            }
            totalSize += bytes.length;
        }

        if (buffer == null || buffer.capacity() < totalSize) {
            buffer = ByteBuffer.allocateDirect(totalSize);
        } else {
            buffer.limit(totalSize);
        }

        for (byte[] bytes : wireData) {
            if (logger.isTraceEnabled()) {
                logger.trace("{}", bytes == null ? "(buffer was null)" : Hexdump.format(bytes));
            }

            if (bytes == null) {
                break;
            }

            buffer.put(bytes);
        }

        if (hasFileDescriptorSupport) {
            List<org.freedesktop.dbus.FileDescriptor> fdsList = _msg.getFiledescriptors();
            if (!fdsList.isEmpty()) {
                try {
                    FileDescriptor[] fds = new FileDescriptor[fdsList.size()];
                    for (int i = 0; i < fdsList.size(); i++) {
                        fds[i] = FileDescriptorCast.unsafeUsing(fdsList.get(i).getIntFileDescriptor()).getFileDescriptor();
                    }

                    socket.setOutboundFileDescriptors(fds);
                } catch (IOException _ex) {
                    logger.error("Unable to extract FileDescriptor", _ex);
                    return;
                }
            }
        }

        buffer.flip();
        socket.write(buffer);
        buffer.clear();

        if (hasFileDescriptorSupport && !_msg.getFiledescriptors().isEmpty()) {
            socket.setOutboundFileDescriptors((FileDescriptor[]) null);
        }

        logger.trace("Message sent: {}", _msg);
    }
}
