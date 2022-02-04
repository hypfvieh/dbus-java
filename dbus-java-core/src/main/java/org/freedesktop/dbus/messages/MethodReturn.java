package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;

public class MethodReturn extends Message {

    private MethodCall call;

    MethodReturn() {
    }

    public MethodReturn(String _dest, long _replyserial, String _sig, Object... _args) throws DBusException {
        this(null, _dest, _replyserial, _sig, _args);
    }

    public MethodReturn(String _source, String _dest, long _replyserial, String _sig, Object... _args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.METHOD_RETURN, (byte) 0);

        getHeaders().put(Message.HeaderField.REPLY_SERIAL, _replyserial);

        List<Object> hargs = new ArrayList<>();
        hargs.add(new Object[] {
                Message.HeaderField.REPLY_SERIAL, new Object[] {
                        ArgumentType.UINT32_STRING, _replyserial
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

        if (null != _sig) {
            hargs.add(new Object[] {
                    Message.HeaderField.SIGNATURE, new Object[] {
                            ArgumentType.SIGNATURE_STRING, _sig
                    }
            });
            getHeaders().put(Message.HeaderField.SIGNATURE, _sig);
            setArgs(_args);
        }

        int totalFileDes = 0;
        for( int x = 0; x < _args.length; x++ ){
            if( _args[x] instanceof FileDescriptor ){
                totalFileDes++;
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
        marshallint(getByteCounter() - c, blen, 0, 4);
    }

    public MethodReturn(MethodCall _mc, String _sig, Object... _args) throws DBusException {
        this(null, _mc, _sig, _args);
    }

    public MethodReturn(String _source, MethodCall _mc, String _sig, Object... _args) throws DBusException {
        this(_source, _mc.getSource(), _mc.getSerial(), _sig, _args);
        this.call = _mc;
    }


    public MethodCall getCall() {
        return call;
    }

    public void setCall(MethodCall _call) {
        this.call = _call;
    }
}
