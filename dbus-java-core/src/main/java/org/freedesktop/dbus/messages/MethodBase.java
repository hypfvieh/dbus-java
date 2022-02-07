package org.freedesktop.dbus.messages;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message.ArgumentType;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MethodBase extends Message {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    MethodBase() {
    }
    
    public MethodBase(byte _endianness, byte _methodCall, byte _flags) throws DBusException {
        super(_endianness, _methodCall, _flags);
    }

    /**
     * Appends filedescriptors (if any) and adds common signatures and padding.
     *  
     * @param _hargs
     * @param _sig
     * @param _args
     * @throws DBusException
     */
    void createCommon(List<Object> _hargs, String _sig, Object... _args) throws DBusException {
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

        byte[] blen = new byte[4];
        appendBytes(blen);
        append("ua(yv)", getSerial(), _hargs.toArray());
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
     * Creates a message header.
     * Will automatically add the values to the current instances header map.
     * 
     * @param _header header type (one of {@link HeaderField})
     * @param _argType arguement type (one of {@link ArgumentType})
     * @param _value value
     * 
     * @return Object array
     */
    Object[] createHeaderArgs(byte _header, String _argType, Object _value) {
        getHeaders().put(_header, _value);
        return new Object[] {
                _header, new Object[] {
                        _argType, _value
                }
        };
    }
}
