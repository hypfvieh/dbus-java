package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageProtocolVersionException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.spi.message.IMessageReader;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import org.newsclub.net.unix.FileDescriptorCast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JUnixSocketMessageReader implements IMessageReader {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AFUNIXSocketChannel socket;
    private final boolean hasFileDescriptorSupport;
    private final int[] len = new int[4];
    private final ByteBuffer buf = ByteBuffer.allocateDirect(12);
    private final ByteBuffer tbuf = ByteBuffer.allocateDirect(4);
    private ByteBuffer header = null;
    private ByteBuffer body = null;

    public JUnixSocketMessageReader(AFUNIXSocketChannel _socket, boolean _hasFileDescriptorSupport) {
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
    public Message readMessage() throws IOException, DBusException {
        /* Read the 12 byte fixed header, retrying as necessary */
        if (len[0] < 12) {
            try {
                int rv = socket.read(buf);
                if (rv < 0) {
                    throw new EOFException("(1) Underlying transport returned " + rv);
                }
                len[0] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[0] == 0) {
            return null;
        }

        if (len[0] < 12) {
            logger.trace("Only got {} of 12 bytes of header", len[0]);
            return null;
        }

        buf.flip();

        /* Ensure protocol version. */
        int protoVer = buf.get(3);

        if (protoVer > Message.PROTOCOL) {
            throw new MessageProtocolVersionException(String.format("Protocol version %s is unsupported", protoVer));
        }

        final ByteOrder byteOrder;
        switch (buf.get(0)) {
            case Message.Endian.BIG:
                byteOrder = ByteOrder.BIG_ENDIAN;
                break;
            case Message.Endian.LITTLE:
                byteOrder = ByteOrder.LITTLE_ENDIAN;
                break;
            default:
                throw new IOException(String.format("Unsupported endian: %s", (char) buf.get(0)));
        }

        buf.order(byteOrder);
        tbuf.order(byteOrder);

        if (len[1] < 4) {
            try {
                int rv = socket.read(tbuf);
                if (rv < 0) {
                    throw new EOFException("(2) Underlying transport returned " + rv);
                }
                len[1] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[1] < 4) {
            logger.trace("Only got {} of 4 bytes of header", len[1]);
            return null;
        }

        tbuf.flip();

        /* Parse the variable header length */
        int headerlen;

        if (header == null) {
            headerlen = tbuf.getInt(0);

            /* n % 2^i = n & (2^i - 1) */
            int modlen = headerlen & 7;
            if (modlen != 0) {
                headerlen += 8 - modlen;
            }

            header = ByteBuffer.allocateDirect(headerlen + 8).put(tbuf).position(8);
            len[2] = 0;
        } else {
            headerlen = header.capacity() - 8;
        }

        if (len[2] < headerlen) {
            try {
                int rv = socket.read(header);
                if (rv < 0) {
                    throw new EOFException("(3) Underlying transport returned " + rv);
                }
                len[2] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[2] < headerlen) {
            logger.trace("Only got {} of {} bytes of header", len[2], headerlen);
            return null;
        }

        header.flip();

        byte type = buf.get(1);

        /* Read the body */
        if (body == null) {
            body = ByteBuffer.allocateDirect(buf.getInt(4));
            len[3] = 0;
        }

        if (len[3] < body.capacity()) {
            try {
                int rv = socket.read(body);
                if (rv < 0) {
                    throw new EOFException("(4) Underlying transport returned " + rv);
                }
                len[3] += rv;
            } catch (SocketTimeoutException _ex) {
                return null;
            }
        }

        if (len[3] < body.capacity()) {
            logger.trace("Only got {} of {} bytes of body", len[3], body.capacity());
            return null;
        }

        body.flip();

        try {
            final List<org.freedesktop.dbus.FileDescriptor> fds;
            if (hasFileDescriptorSupport) {
                FileDescriptor[] receivedFileDescriptors = socket.getReceivedFileDescriptors();
                if (receivedFileDescriptors.length == 0) {
                    fds = null;
                } else {
                    fds = new ArrayList<>(receivedFileDescriptors.length);
                    for (int i = 0; i < receivedFileDescriptors.length; i++) {
                        fds.set(i, new org.freedesktop.dbus.FileDescriptor(FileDescriptorCast.using(receivedFileDescriptors[i]).as(Integer.class)));
                    }
                }
            } else {
                fds = null;
            }
            if (fds != null) {
                logger.debug("=> {}", fds);
            }
            return MessageFactory.createMessage(type,
                    remaining(buf),
                    remaining(header),
                    remaining(body),
                    fds);
        } catch (DBusException | RuntimeException _ex) {
            logger.warn("Exception while creating message.", _ex);
            throw _ex;
        } finally {
            tbuf.rewind();
            buf.rewind();
            Arrays.fill(len, 0);
            body = null;
            header = null;
        }
    }

    private static byte[] remaining(ByteBuffer _buffer) {
        byte[] bytes = new byte[_buffer.remaining()];
        _buffer.get(bytes);
        return bytes;
    }
}
