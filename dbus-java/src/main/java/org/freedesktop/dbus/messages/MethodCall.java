/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.Hexdump;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.types.UInt32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodCall extends Message {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    MethodCall() {
    }

    public MethodCall(String dest, String path, String iface, String member, byte flags, String sig, Object... args) throws DBusException {
        this(null, dest, path, iface, member, flags, sig, args);
    }

    public MethodCall(String source, String dest, String path, String iface, String member, byte flags, String sig, Object... args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.METHOD_CALL, flags);

        if (null == member || null == path) {
            throw new MessageFormatException("Must specify destination, path and function name to MethodCalls.");
        }
        getHeaders().put(Message.HeaderField.PATH, path);
        getHeaders().put(Message.HeaderField.MEMBER, member);

        List<Object> hargs = new ArrayList<>();

        hargs.add(new Object[] {
                Message.HeaderField.PATH, new Object[] {
                        ArgumentType.OBJECT_PATH_STRING, path
                }
        });

        if (null != source) {
            getHeaders().put(Message.HeaderField.SENDER, source);
            hargs.add(new Object[] {
                    Message.HeaderField.SENDER, new Object[] {
                            ArgumentType.STRING_STRING, source
                    }
            });
        }

        if (null != dest) {
            getHeaders().put(Message.HeaderField.DESTINATION, dest);
            hargs.add(new Object[] {
                    Message.HeaderField.DESTINATION, new Object[] {
                            ArgumentType.STRING_STRING, dest
                    }
            });
        }

        if (null != iface) {
            hargs.add(new Object[] {
                    Message.HeaderField.INTERFACE, new Object[] {
                            ArgumentType.STRING_STRING, iface
                    }
            });
            getHeaders().put(Message.HeaderField.INTERFACE, iface);
        }

        hargs.add(new Object[] {
                Message.HeaderField.MEMBER, new Object[] {
                        ArgumentType.STRING_STRING, member
                }
        });

        if (null != sig) {
            logger.debug("Appending arguments with signature: {}", sig);
            hargs.add(new Object[] {
                    Message.HeaderField.SIGNATURE, new Object[] {
                            ArgumentType.SIGNATURE_STRING, sig
                    }
            });
            getHeaders().put(Message.HeaderField.SIGNATURE, sig);
            setArgs(args);
        }

        int totalFileDes = 0;
        if( args != null ){
            for( int x = 0; x < args.length; x++ ){
                if( args[x] instanceof FileDescriptor ){
                    totalFileDes++;
                }
            }
        }

        if( totalFileDes > 0 ){
            getHeaders().put(Message.HeaderField.UNIX_FDS, totalFileDes);
            hargs.add(new Object[]{
                    Message.HeaderField.UNIX_FDS, new Object[]{
                    ArgumentType.UINT32_STRING, new UInt32( totalFileDes )
                }
            });
        }

        byte[] blen = new byte[4];
        appendBytes(blen);
        append("ua(yv)", getSerial(), hargs.toArray());
        pad((byte) 8);

        long c = getByteCounter();
        if (null != sig) {
            append(sig, args);
        }
        logger.debug("Appended body, type: {} start: {} end: {} size: {}",sig, c, getByteCounter(), (getByteCounter() - c));
        marshallint(getByteCounter() - c, blen, 0, 4);
        logger.debug("marshalled size ({}): {}",blen, Hexdump.format(blen));
    }

    private static long REPLY_WAIT_TIMEOUT = 200000;

    /**
    * Set the default timeout for method calls.
    * Default is 20s.
    * @param timeout New timeout in ms.
    */
    public static void setDefaultTimeout(long timeout) {
        REPLY_WAIT_TIMEOUT = timeout;
    }

    // CHECKSTYLE:OFF
    Message reply = null;
    // CHECKSTYLE:ON

    public synchronized boolean hasReply() {
        return null != reply;
    }

    /**
    * Block (if neccessary) for a reply.
    * @return The reply to this MethodCall, or null if a timeout happens.
    * @param timeout The length of time to block before timing out (ms).
    */
    public synchronized Message getReply(long timeout) {
        logger.trace("Blocking on {}", this);
        if (null != reply) {
            return reply;
        }
        try {
            wait(timeout);
            return reply;
        } catch (InterruptedException exI) {
            return reply;
        }
    }

    /**
    * Block (if neccessary) for a reply.
    * Default timeout is 20s, or can be configured with setDefaultTimeout()
    * @return The reply to this MethodCall, or null if a timeout happens.
    */
    public synchronized Message getReply() {
        logger.trace("Blocking on {}", this);

        if (null != reply) {
            return reply;
        }
        try {
            wait(REPLY_WAIT_TIMEOUT);
            return reply;
        } catch (InterruptedException exI) {
            return reply;
        }
    }

    public synchronized void setReply(Message _reply) {
        logger.trace("Setting reply to {} to {}", this, _reply);
        this.reply = _reply;
        notifyAll();
    }

}
