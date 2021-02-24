package org.freedesktop.dbus.spi;

import java.io.IOException;
import java.io.OutputStream;

import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputStreamMessageWriter implements IMessageWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OutputStream outputStream;

    public OutputStreamMessageWriter(OutputStream _out) {
        this.outputStream = _out;
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
            outputStream.write(buf);
        }
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        logger.debug("Closing Message Writer");
        if (outputStream != null) {
            outputStream.close();
        }
        outputStream = null;
    }

    @Override
    public boolean isClosed() {
        return outputStream == null;
    }
}
