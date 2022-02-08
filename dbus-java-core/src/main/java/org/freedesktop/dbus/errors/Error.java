package org.freedesktop.dbus.errors;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.MessageFormatException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Error messages which can be sent over the bus.
 */
public class Error extends Message {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Error() {
    }

    public Error(String _dest, String _errorName, long _replyserial, String _sig, Object... _args) throws DBusException {
        this(null, _dest, _errorName, _replyserial, _sig, _args);
    }

    public Error(String _source, String _dest, String _errorName, long _replyserial, String _sig, Object... _args)
            throws DBusException {
        super(DBusConnection.getEndianness(), Message.MessageType.ERROR, (byte) 0);

        if (null == _errorName) {
            throw new MessageFormatException("Must specify error name to Errors.");
        }

        List<Object> hargs = new ArrayList<>();
        hargs.add(createHeaderArgs(HeaderField.ERROR_NAME, ArgumentType.STRING_STRING, _errorName));
        hargs.add(createHeaderArgs(HeaderField.REPLY_SERIAL, ArgumentType.UINT32_STRING, _replyserial));
        
        if (null != _source) {
            hargs.add(createHeaderArgs(HeaderField.SENDER, ArgumentType.STRING_STRING, _source));
        }

        if (null != _dest) {
            hargs.add(createHeaderArgs(HeaderField.DESTINATION, ArgumentType.STRING_STRING, _dest));
        }

        if (null != _sig) {
            hargs.add(createHeaderArgs(HeaderField.SIGNATURE, ArgumentType.SIGNATURE_STRING, _sig));
            setArgs(_args);
        }

        padAndMarshall(hargs, getSerial(), _sig, _args);
    }

    public Error(String _source, Message _m, Throwable _ex) throws DBusException {
        this(_source, _m.getSource(), AbstractConnection.DOLLAR_PATTERN.matcher(_ex.getClass().getName()).replaceAll("."),
                _m.getSerial(), "s", _ex.getMessage());
    }

    public Error(Message _m, Throwable _ex) throws DBusException {
        this(_m.getSource(), AbstractConnection.DOLLAR_PATTERN.matcher(_ex.getClass().getName()).replaceAll("."),
                _m.getSerial(), "s", _ex.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DBusExecutionException> createExceptionClass(String _name) {
        Class<? extends DBusExecutionException> c = null;

        // Fix package name for DBus own error messages
        if (_name.startsWith("org.freedesktop.DBus.Error.")) {
            _name = _name.replace("org.freedesktop.DBus.Error.", "org.freedesktop.dbus.errors.");
        }

        do {
            try {
                c = (Class<? extends DBusExecutionException>) Class.forName(_name);
            } catch (ClassNotFoundException _exCnf) {
            }
            _name = _name.replaceAll("\\.([^\\.]*)$", "\\$$1");
        } while (null == c && _name.matches(".*\\..*"));
        return c;
    }

    /**
     * Turns this into an exception of the correct type
     *
     * @return exception
     */
    public DBusExecutionException getException() {
        try {
            Class<? extends DBusExecutionException> c = createExceptionClass(getName());
            if (null == c || !DBusExecutionException.class.isAssignableFrom(c)) {
                c = DBusExecutionException.class;
            }
            Constructor<? extends DBusExecutionException> con = c.getConstructor(String.class);
            DBusExecutionException ex;
            Object[] args = getParameters();
            if (null == args || 0 == args.length) {
                ex = con.newInstance("");
            } else {
                String s = "";
                for (Object o : args) {
                    s += o + " ";
                }
                ex = con.newInstance(s.trim());
            }
            ex.setType(getName());
            return ex;
        } catch (Exception _ex1) {
            logger.debug("", _ex1);
            DBusExecutionException ex;
            Object[] args = null;
            try {
                args = getParameters();
            } catch (Exception _ex2) {
            }
            if (null == args || 0 == args.length) {
                ex = new DBusExecutionException("");
            } else {
                String s = "";
                for (Object o : args) {
                    s += o + " ";
                }
                ex = new DBusExecutionException(s.trim());
            }
            ex.setType(getName());
            return ex;
        }
    }

    /**
     * Throw this as an exception of the correct type
     */
    public void throwException() throws DBusExecutionException {
        throw getException();
    }
}
