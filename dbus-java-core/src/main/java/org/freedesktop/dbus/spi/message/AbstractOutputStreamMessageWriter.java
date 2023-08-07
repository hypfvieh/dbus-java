package org.freedesktop.dbus.spi.message;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Objects;

public abstract class AbstractOutputStreamMessageWriter implements IMessageWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SocketChannel outputChannel;
    private final boolean       hasFileDescriptorSupport;

    public AbstractOutputStreamMessageWriter(final SocketChannel _out, boolean _fileDescriptorSupport) {
        outputChannel = Objects.requireNonNull(_out, "SocketChannel required");
        hasFileDescriptorSupport = _fileDescriptorSupport;
    }

    @Override
    public final void writeMessage(Message _msg) throws IOException {
        logger.debug("<= {}", _msg);
        if (null == _msg) {
            return;
        }
        if (null == _msg.getWireData()) {
            logger.warn("Message {} wire-data was null!", _msg);
            return;
        }

        for (byte[] buf : _msg.getWireData()) {
            if (logger.isTraceEnabled()) {
                logger.trace("{}", null == buf ? "(buffer was null)" : Hexdump.format(buf));
            }
            if (null == buf) {
                break;
            }

            outputChannel.write(ByteBuffer.wrap(buf));
        }
        
        if (hasFileDescriptorSupport) {
            writeFileDescriptors(outputChannel, _msg.getFiledescriptors());
        }
        
        logger.trace("Message sent: {}", _msg);
    }

    /**
     * Called to write any file descriptors to the given channel.<br<
     * Should do nothing if there is no file descriptor to write, or method is not supported.
     * 
     * @param _outputChannel channel to write to
     * @param _filedescriptors file descriptors attached to message
     * 
     * @throws IOException when writing the descriptors fail
     */
    protected abstract void writeFileDescriptors(SocketChannel _outputChannel, List<FileDescriptor> _filedescriptors) throws IOException;

    @Override
    public void close() throws IOException {
        logger.debug("Closing Message Writer");
        if (outputChannel.isOpen()) {
            outputChannel.close();
            logger.debug("Message Writer closed");
        }
    }

    @Override
    public boolean isClosed() {
        return !outputChannel.isOpen();
    }
    
}
