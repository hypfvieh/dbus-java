/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.freedesktop.Hexdump;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageWriter implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OutputStream outputStream;

    public MessageWriter(OutputStream _out) {
        this.outputStream = _out;
    }

    public void writeMessage(Message m) throws IOException {
        logger.debug("<= {}", m);
        if (null == m) {
            return;
        }
        if (null == m.getWireData()) {
            logger.warn("Message {} wire-data was null!", m);
            return;
        }

        logger.debug("Writing {} bytes for signature '{}' simultaneously to Unix Socket", m.getWireData().length, m.getSig());
        for (byte[] buf : m.getWireData()) {
            logger.trace("({}):{}", buf, (null == buf ? "" : Hexdump.format(buf)));
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

    public boolean isClosed() {
        return outputStream == null;
    }
}
