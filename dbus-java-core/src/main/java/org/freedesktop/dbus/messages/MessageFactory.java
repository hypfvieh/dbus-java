package org.freedesktop.dbus.messages;

import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageTypeException;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class MessageFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageFactory.class);

    private MessageFactory() {}

    public static Message createMessage(byte _type, byte[] _buf, byte[] _header, byte[] _body, List<FileDescriptor> _filedescriptors) throws DBusException, MessageTypeException {
        Message m = switch (_type) {
            case Message.MessageType.METHOD_CALL -> new MethodCall();
            case Message.MessageType.METHOD_RETURN -> new MethodReturn();
            case Message.MessageType.SIGNAL -> new DBusSignal();
            case Message.MessageType.ERROR -> new Error();
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
