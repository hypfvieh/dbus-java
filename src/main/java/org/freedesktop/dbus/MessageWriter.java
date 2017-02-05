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
import java.io.IOException;
import java.io.OutputStream;

import org.freedesktop.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cx.ath.matthew.unix.USOutputStream;

public class MessageWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OutputStream out;
    private boolean      isunix;

    public MessageWriter(OutputStream out) {
        this.out = out;
        this.isunix = false;
        try {
            if (out instanceof USOutputStream) {
                this.isunix = true;
            }
        } catch (Throwable t) {
        }
        if (!this.isunix) {
            this.out = new BufferedOutputStream(this.out);
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
        if (isunix) {
            if (logger.isTraceEnabled()) {
                logger.debug("Writing all " + m.getWireData().length + " buffers simultaneously to Unix Socket");
                for (byte[] buf : m.getWireData()) {
                    logger.trace("(" + buf + "):" + (null == buf ? "" : Hexdump.format(buf)));
                }
            }
            ((USOutputStream) out).write(m.getWireData());
        } else {
            for (byte[] buf : m.getWireData()) {
                logger.trace("(" + buf + "):" + (null == buf ? "" : Hexdump.format(buf)));
                if (null == buf) {
                    break;
                }
                out.write(buf);
            }
        }
        out.flush();
    }

    public void close() throws IOException {
        logger.info("Closing Message Writer");
        out.close();
    }
}
