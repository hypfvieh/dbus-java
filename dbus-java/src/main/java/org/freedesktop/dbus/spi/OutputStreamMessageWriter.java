package org.freedesktop.dbus.spi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputStreamMessageWriter implements IMessageWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SocketChannel outputChannel;

    public OutputStreamMessageWriter(SocketChannel _out) {
        this.outputChannel = _out;
    }

    @Override
    public void writeMessage(Message m) throws IOException {
        logger.debug("<= {}", m);
        if (null == m) {
            return;
        }
        if (null == m.getWireData()) {
            logger.warn("Message {} wire-data was null!", m);
            return;
        }

        for (byte[] buf : m.getWireData()) {
            if(logger.isTraceEnabled()) {
                logger.trace("{}", null == buf ? "" : Hexdump.format(buf));
            }
            if (null == buf) {
                break;
            }

            outputChannel.write(ByteBuffer.wrap(buf));
        }
    }

    @Override
    public void close() throws IOException {
        logger.debug("Closing Message Writer");
        if (outputChannel != null) {
            outputChannel.close();
        }
        outputChannel = null;
    }

    @Override
    public boolean isClosed() {
        return outputChannel == null;
    }
}
