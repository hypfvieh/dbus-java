package org.freedesktop.dbus.messages;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageTypeException;
import org.freedesktop.dbus.messages.constants.MessageType;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class MessageFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFactory.class);
    private final byte          endianess;

    public MessageFactory(byte _endianess) {
        endianess = _endianess;
    }

    public byte getEndianess() {
        return endianess;
    }

    public DBusSignal createSignal(String _source, String _path, String _iface, String _member, String _sig, Object... _args) throws DBusException {
        return new DBusSignal(endianess, _source, _path, _iface, _member, _sig, _args);
    }

    public DBusSignal createSignal(String _objectPath, Object... _args) throws DBusException {
        var sig = new DBusSignal(_objectPath, _args);
        sig.updateEndianess(endianess);
        return sig;
    }

    public MethodCall createMethodCall(String _source, String _dest, String _path, String _iface, String _member, byte _flags, String _sig, Object... _args) throws DBusException {
        return new MethodCall(endianess, _source, _dest, _path, _iface, _member, _flags, _sig, _args);
    }

    public MethodCall createMethodCall(String _dest, String _path, String _iface, String _member, byte _flags, String _sig, Object... _args) throws DBusException {
        return new MethodCall(endianess, _dest, _path, _iface, _member, _flags, _sig, _args);
    }

    public MethodReturn createMethodReturn(MethodCall _mc, String _sig, Object... _args) throws DBusException {
        return new MethodReturn(_mc, _sig, _args);
    }

    public MethodReturn createMethodReturn(String _source, MethodCall _mc, String _sig, Object... _args) throws DBusException {
        return new MethodReturn(_source, _mc, _sig, _args);
    }

    public Error createError(Message _m, Throwable _ex) throws DBusException {
        return new Error(endianess, _m, _ex);
    }

    public Error createError(String _source, Message _m, Throwable _ex) throws DBusException {
        return new Error(endianess, _source, _m, _ex);
    }

    public Error createError(String _dest, String _errorName, long _replyserial, String _sig, Object... _args) throws DBusException {
        return new Error(endianess, _dest, _errorName, _replyserial, _sig, _args);
    }

    public Error createError(String _source, String _dest, String _errorName, long _replyserial, String _sig, Object... _args) throws DBusException {
        return new Error(endianess, _source, _dest, _errorName, _replyserial, _sig, _args);
    }

    public static Message createMessage(byte _type, byte[] _buf, byte[] _header, byte[] _body, List<FileDescriptor> _filedescriptors) throws DBusException, MessageTypeException {
        Message m = switch (_type) {
            case MessageType.METHOD_CALL -> new MethodCall();
            case MessageType.METHOD_RETURN -> new MethodReturn();
            case MessageType.SIGNAL -> new DBusSignal();
            case MessageType.ERROR -> new Error();
            default -> throw new MessageTypeException(String.format("Message type %s unsupported", _type));
        };

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(Hexdump.format(_buf));
            LOGGER.trace(Hexdump.format(_header));
            LOGGER.trace(Hexdump.format(_body));
        }

        m.populate(_buf, _header, _body, _filedescriptors);
        return m;
    }
}
