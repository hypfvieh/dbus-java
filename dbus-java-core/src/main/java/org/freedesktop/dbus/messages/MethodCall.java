package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodCall extends Message {
    private static long  REPLY_WAIT_TIMEOUT = 200000;

    private final Logger logger             = LoggerFactory.getLogger(getClass());

    // CHECKSTYLE:OFF
    Message              reply              = null;
    // CHECKSTYLE:ON

    MethodCall() {
    }

    public MethodCall(String _dest, String _path, String _iface, String _member, byte _flags, String _sig, Object... _args) throws DBusException {
        this(null, _dest, _path, _iface, _member, _flags, _sig, _args);
    }

    public MethodCall(String _source, String _dest, String _path, String _iface, String _member, byte _flags, String _sig, Object... _args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.METHOD_CALL, _flags);

        if (null == _member || null == _path) {
            throw new MessageFormatException("Must specify destination, path and function name to MethodCalls.");
        }
        getHeaders().put(Message.HeaderField.PATH, _path);
        getHeaders().put(Message.HeaderField.MEMBER, _member);

        List<Object> hargs = new ArrayList<>();

        hargs.add(new Object[] {
                Message.HeaderField.PATH, new Object[] {
                        ArgumentType.OBJECT_PATH_STRING, _path
                }
        });

        if (null != _source) {
            getHeaders().put(Message.HeaderField.SENDER, _source);
            hargs.add(new Object[] {
                    Message.HeaderField.SENDER, new Object[] {
                            ArgumentType.STRING_STRING, _source
                    }
            });
        }

        if (null != _dest) {
            getHeaders().put(Message.HeaderField.DESTINATION, _dest);
            hargs.add(new Object[] {
                    Message.HeaderField.DESTINATION, new Object[] {
                            ArgumentType.STRING_STRING, _dest
                    }
            });
        }

        if (null != _iface) {
            hargs.add(new Object[] {
                    Message.HeaderField.INTERFACE, new Object[] {
                            ArgumentType.STRING_STRING, _iface
                    }
            });
            getHeaders().put(Message.HeaderField.INTERFACE, _iface);
        }

        hargs.add(new Object[] {
                Message.HeaderField.MEMBER, new Object[] {
                        ArgumentType.STRING_STRING, _member
                }
        });

        if (null != _sig) {
            logger.debug("Appending arguments with signature: {}", _sig);
            hargs.add(new Object[] {
                    Message.HeaderField.SIGNATURE, new Object[] {
                            ArgumentType.SIGNATURE_STRING, _sig
                    }
            });
            getHeaders().put(Message.HeaderField.SIGNATURE, _sig);
            setArgs(_args);
        }

        int totalFileDes = 0;
        if( _args != null ){
            for( int x = 0; x < _args.length; x++ ){
                if( _args[x] instanceof FileDescriptor ){
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
        if (null != _sig) {
            append(_sig, _args);
        }
        logger.debug("Appended body, type: {} start: {} end: {} size: {}",_sig, c, getByteCounter(), getByteCounter() - c);
        marshallint(getByteCounter() - c, blen, 0, 4);
        logger.debug("marshalled size ({}): {}",blen, Hexdump.format(blen));
    }

    /**
    * Set the default timeout for method calls.
    * Default is 20s.
    * @param _timeout New timeout in ms.
    */
    public static void setDefaultTimeout(long _timeout) {
        REPLY_WAIT_TIMEOUT = _timeout;
    }

    
    public synchronized boolean hasReply() {
        return null != reply;
    }

    /**
    * Block (if neccessary) for a reply.
    * @return The reply to this MethodCall, or null if a timeout happens.
    * @param _timeout The length of time to block before timing out (ms).
    */
    public synchronized Message getReply(long _timeout) {
        logger.trace("Blocking on {}", this);
        if (null != reply) {
            return reply;
        }
        try {
            wait(_timeout);
            return reply;
        } catch (InterruptedException _exI) {
            Thread.currentThread().interrupt(); // keep interrupted state
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
            Thread.currentThread().interrupt(); // keep interrupted state
            return reply;
        }
    }

    public synchronized void setReply(Message _reply) {
        logger.trace("Setting reply to {} to {}", this, _reply);
        this.reply = _reply;
        notifyAll();
    }

}
