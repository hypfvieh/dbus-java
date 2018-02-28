/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cx.ath.matthew.unix.USOutputStream;
import cx.ath.matthew.utils.Hexdump;

public class MessageWriter implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OutputStream outputStream;
    private boolean      unixSocket;

    public MessageWriter(OutputStream _out) {
        this.outputStream = _out;
        this.unixSocket = false;
        try {
            if (_out instanceof USOutputStream) {
                this.unixSocket = true;
            }
        } catch (Throwable t) {
        }
        if (!this.unixSocket) {
            this.outputStream = new BufferedOutputStream(_out);
        }
    }

    public void writeMessage(Message m) throws IOException {
        logger.info("<= " + m);
        if (null == m) {
            return;
        }
        if (null == m.getWireData()) {
            logger.warn("Message " + m + " wire-data was null!");
            return;
        }
        if (unixSocket) {
            if (logger.isTraceEnabled()) {
                logger.debug("Writing all " + m.getWireData().length + " buffers simultaneously to Unix Socket");
                for (byte[] buf : m.getWireData()) {
                    logger.trace("(" + buf + "):" + (null == buf ? "" : Hexdump.format(buf)));
                }
            }
            ((USOutputStream) outputStream).write(m.getWireData());
        } else {
            for (byte[] buf : m.getWireData()) {
                logger.trace("(" + buf + "):" + (null == buf ? "" : Hexdump.format(buf)));
                if (null == buf) {
                    break;
                }
                outputStream.write(buf);
            }
        }
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing Message Writer");
        if (outputStream != null) {
            outputStream.close();
        }
        outputStream = null;
    }

    public boolean isClosed() {
        return outputStream != null;
    }
}
