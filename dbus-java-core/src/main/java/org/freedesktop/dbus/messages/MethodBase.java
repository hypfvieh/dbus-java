package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;

public abstract class MethodBase extends Message {
    
    
    MethodBase() {
    }
    
    public MethodBase(byte _endianness, byte _methodCall, byte _flags) throws DBusException {
        super(_endianness, _methodCall, _flags);
    }

    /**
     * Appends filedescriptors (if any).
     *  
     * @param _hargs
     * @param _sig
     * @param _args
     * @throws DBusException
     */
    void appendFileDescriptors(List<Object> _hargs, String _sig, Object... _args) throws DBusException {
        if (_hargs == null) {
            _hargs = new ArrayList<>();
        }
        
        int totalFileDes = 0;
        if (_args != null) {
            for (int x = 0; x < _args.length; x++) {
                if (_args[x] instanceof FileDescriptor) {
                    totalFileDes++;
                }
            }
        }

        if (totalFileDes > 0) {
            _hargs.add(createHeaderArgs(Message.HeaderField.UNIX_FDS, ArgumentType.UINT32_STRING, new UInt32(totalFileDes)));
        }

    }
}
