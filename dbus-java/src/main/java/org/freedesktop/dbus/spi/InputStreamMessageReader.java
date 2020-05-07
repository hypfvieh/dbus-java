/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.spi;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageProtocolVersionException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputStreamMessageReader implements IMessageReader {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private InputStream inputStream;
    private byte[]      buf    = null;
    private byte[]      tbuf   = null;
    private byte[]      header = null;
    private byte[]      body   = null;
    private int[]       len    = new int[4];

    public InputStreamMessageReader(InputStream _in) {
        this.inputStream = new BufferedInputStream(_in);
    }

    @Override
    public Message readMessage() throws IOException, DBusException {
        int rv;
        /* Read the 12 byte fixed header, retrying as necessary */
        if (null == buf) {
            buf = new byte[12];
            len[0] = 0;
        }
        if (len[0] < 12) {
            try {
                rv = inputStream.read(buf, len[0], 12 - len[0]);
            } catch (SocketTimeoutException exSt) {
                return null;
            } catch (EOFException _ex) {
                return null;
            } catch (IOException _ex) {
                throw _ex;
            }
            if (-1 == rv) {
                throw new EOFException("Underlying transport returned EOF (1)");
            }
            len[0] += rv;
        }
        if (len[0] == 0) {
            return null;
        }
        if (len[0] < 12) {
            logger.debug("Only got {} of 12 bytes of header", len[0]);
            return null;
        }

        /* Parse the details from the header */
        byte endian = buf[0];
        byte type = buf[1];
        byte protover = buf[3];
        if (protover > Message.PROTOCOL) {
            buf = null;
            throw new MessageProtocolVersionException(String.format("Protocol version %s is unsupported", protover));
        }

        /* Read the length of the variable header */
        if (null == tbuf) {
            tbuf = new byte[4];
            len[1] = 0;
        }
        if (len[1] < 4) {
            try {
                rv = inputStream.read(tbuf, len[1], 4 - len[1]);
            } catch (SocketTimeoutException exSt) {
                return null;
            }
            if (-1 == rv) {
                throw new EOFException("Underlying transport returned EOF (2)");
            }
            len[1] += rv;
        }
        if (len[1] < 4) {
            logger.debug("Only got {} of 4 bytes of header", len[1]);
            return null;
        }

        /* Parse the variable header length */
        int headerlen = 0;
        if (null == header) {
            headerlen = (int) Message.demarshallint(tbuf, 0, endian, 4);
            if (0 != headerlen % 8) {
                headerlen += 8 - (headerlen % 8);
            }
        } else {
            headerlen = header.length - 8;
        }

        /* Read the variable header */
        if (null == header) {
            header = new byte[headerlen + 8];
            System.arraycopy(tbuf, 0, header, 0, 4);
            len[2] = 0;
        }
        if (len[2] < headerlen) {
            try {
                rv = inputStream.read(header, 8 + len[2], headerlen - len[2]);
            } catch (SocketTimeoutException exSt) {
                return null;
            }
            if (-1 == rv) {
                throw new EOFException("Underlying transport returned EOF (3)");
            }
            len[2] += rv;
        }
        if (len[2] < headerlen) {
            logger.debug("Only got {} of {} bytes of header", len[2], headerlen);
            return null;
        }

        /* Read the body */
        int bodylen = 0;
        if (null == body) {
            bodylen = (int) Message.demarshallint(buf, 4, endian, 4);
        }
        if (null == body) {
            body = new byte[bodylen];
            len[3] = 0;
        }
        if (len[3] < body.length) {
            try {
                rv = inputStream.read(body, len[3], body.length - len[3]);
            } catch (SocketTimeoutException exSt) {
                return null;
            }
            if (-1 == rv) {
                throw new EOFException("Underlying transport returned EOF (4)");
            }
            len[3] += rv;
        }
        if (len[3] < body.length) {
            logger.debug("Only got {} of {} bytes of body", len[3], body.length);
            return null;
        }

        Message m;
        try {
            m = MessageFactory.createMessage(type, buf, header, body, null);
        } catch (DBusException dbe) {
            logger.debug("", dbe);
            buf = null;
            tbuf = null;
            body = null;
            header = null;
            throw dbe;
        } catch (RuntimeException exRe) { // this really smells badly!
            logger.debug("", exRe);
            buf = null;
            tbuf = null;
            body = null;
            header = null;
            throw exRe;
        }
        logger.debug("=> {}", m);
        buf = null;
        tbuf = null;
        body = null;
        header = null;
        return m;
    }

    @Override
    public void close() throws IOException {
        logger.trace("Closing Message Reader");
        if (inputStream != null) {
            inputStream.close();
        }
        inputStream = null;
    }

    @Override
    public boolean isClosed() {
        return inputStream == null;
    }
}
