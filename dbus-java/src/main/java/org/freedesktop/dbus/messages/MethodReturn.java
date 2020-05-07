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
import org.freedesktop.dbus.FileDescriptor;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;

public class MethodReturn extends Message {
    
    private MethodCall call;
    
    MethodReturn() {
    }

    public MethodReturn(String dest, long replyserial, String sig, Object... args) throws DBusException {
        this(null, dest, replyserial, sig, args);
    }

    public MethodReturn(String source, String dest, long replyserial, String sig, Object... args) throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.METHOD_RETURN, (byte) 0);

        getHeaders().put(Message.HeaderField.REPLY_SERIAL, replyserial);

        List<Object> hargs = new ArrayList<>();
        hargs.add(new Object[] {
                Message.HeaderField.REPLY_SERIAL, new Object[] {
                        ArgumentType.UINT32_STRING, replyserial
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

        if (null != sig) {
            hargs.add(new Object[] {
                    Message.HeaderField.SIGNATURE, new Object[] {
                            ArgumentType.SIGNATURE_STRING, sig
                    }
            });
            getHeaders().put(Message.HeaderField.SIGNATURE, sig);
            setArgs(args);
        }

        int totalFileDes = 0;
        for( int x = 0; x < args.length; x++ ){
            if( args[x] instanceof FileDescriptor ){
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
        if (null != sig) {
            append(sig, args);
        }
        marshallint(getByteCounter() - c, blen, 0, 4);
    }

    public MethodReturn(MethodCall mc, String sig, Object... args) throws DBusException {
        this(null, mc, sig, args);
    }

    public MethodReturn(String source, MethodCall mc, String sig, Object... args) throws DBusException {
        this(source, mc.getSource(), mc.getSerial(), sig, args);
        this.call = mc;
    }

    
    public MethodCall getCall() {
        return call;
    }

    public void setCall(MethodCall _call) {
        this.call = _call;
    }
}
