package org.freedesktop.dbus.messages;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.ArrayFrob;
import org.freedesktop.dbus.Container;
import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.exceptions.UnknownTypeCodeException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.Hexdump;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass of all messages which are sent over the Bus. This class deals with all the marshalling to/from the wire
 * format.
 */
public class Message {
    public static final int  MAXIMUM_ARRAY_LENGTH   = 67108864;
    public static final int  MAXIMUM_MESSAGE_LENGTH = MAXIMUM_ARRAY_LENGTH * 2;
    public static final int  MAXIMUM_NUM_UNIX_FDS   = MAXIMUM_MESSAGE_LENGTH / 4;

    /** The current protocol major version. */
    public static final byte PROTOCOL               = 1;

    /** Position of data offset in int array. */
    private static final int OFFSET_DATA            = 1;
    /** Position of signature offset in int array. */
    private static final int OFFSET_SIG             = 0;

    /** Keep a static reference to each size of padding array to prevent allocation. */
    private static byte[][]  padding;
    static {
        padding = new byte[][] {
                null, new byte[1], new byte[2], new byte[3], new byte[4], new byte[5], new byte[6], new byte[7]
        };
    }
    /** Steps to increment the buffer array. */
    private static final int           BUFFERINCREMENT = 20;

    private static long                globalserial    = 0;

    protected final Logger             logger          = LoggerFactory.getLogger(getClass());

    private final List<FileDescriptor> filedescriptors = new ArrayList<>();
    private final Map<Byte, Object>    headers         = new HashMap<>();

    private byte[][]                   wiredata        = new byte[BUFFERINCREMENT][];
    private long                       bytecounter     = 0;

    private long                       serial;
    private byte                       type;
    private byte                       flags;
    private byte                       protover;

    private boolean                    big;
    private Object[]                   args;
    private byte[]                     body;
    private long                       bodylen         = 0;
    private int                        preallocated    = 0;
    private int                        paofs           = 0;
    private byte[]                     pabuf;
    private int                        bufferuse       = 0;

    /**
     * Create a message; only to be called by sub-classes.
     *
     * @param _endian The endianness to create the message.
     * @param _type The message type.
     * @param _flags Any message flags.
     * @throws DBusException on error
     */
    protected Message(byte _endian, byte _type, byte _flags) throws DBusException {
        this();
        big = Endian.BIG == _endian;
        synchronized (Message.class) {
            serial = ++globalserial;
        }

        logger.debug("Creating message with serial {}", serial);

        this.type = _type;
        this.flags = _flags;
        preallocate(4);
        append("yyyy", _endian, _type, _flags, Message.PROTOCOL);
    }

    /**
     * Create a blank message. Only to be used when calling populate.
     */
    protected Message() {
    }

    /**
     * Create a message from wire-format data.
     *
     * @param _msg D-Bus serialized data of type yyyuu
     * @param _headers D-Bus serialized data of type a(yv)
     * @param _body D-Bus serialized data of the signature defined in headers.
     */
    @SuppressWarnings("unchecked")
    void populate(byte[] _msg, byte[] _headers, byte[] _body, List<FileDescriptor> _descriptors) throws DBusException {
        big = _msg[0] == Endian.BIG;
        type = _msg[1];
        flags = _msg[2];
        protover = _msg[3];
        wiredata[0] = _msg;
        wiredata[1] = _headers;
        wiredata[2] = _body;
        this.body = _body;
        bufferuse = 3;
        bodylen = ((Number) extract(Message.ArgumentType.UINT32_STRING, _msg, 4)[0]).longValue();
        serial = ((Number) extract(Message.ArgumentType.UINT32_STRING, _msg, 8)[0]).longValue();
        bytecounter = _msg.length + _headers.length + _body.length;
        filedescriptors.clear();
        if (_descriptors != null) {
            filedescriptors.addAll(_descriptors);
        }

        LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("Message header: {}", Hexdump.toAscii(_headers)));

        Object[] hs = extract("a(yv)", _headers, 0);

        LoggingHelper.logIf(logger.isTraceEnabled(), () -> logger.trace("Extracted objects: {}", LoggingHelper.arraysDeepString(logger.isTraceEnabled(), hs)));

        for (Object o : (List<Object>) hs[0]) {
            this.headers.put((Byte) ((Object[]) o)[0], ((Variant<Object>) ((Object[]) o)[1]).getValue());
        }
    }

    protected Map<Byte, Object> getHeaders() {
        return headers;
    }

    protected long getByteCounter() {
        return bytecounter;
    }

    protected void setByteCounter(long _bytecounter) {
        bytecounter = _bytecounter;
    }

    protected void setSerial(long _serial) {
        serial = _serial;
    }

    protected byte[][] getWiredata() {
        return wiredata;
    }

    protected void setWiredata(byte[][] _wiredata) {
        wiredata = _wiredata;
    }

    /**
     * Create a buffer of num bytes. Data is copied to this rather than added to the buffer list.
     */
    private void preallocate(int _num) {
        preallocated = 0;
        pabuf = new byte[_num];
        appendBytes(pabuf);
        preallocated = _num;
        paofs = 0;
    }

    /**
     * Ensures there are enough free buffers.
     *
     * @param _num number of free buffers to create.
     */
    private void ensureBuffers(int _num) {
        int increase = _num - wiredata.length + bufferuse;
        if (increase > 0) {
            if (increase < BUFFERINCREMENT) {
                increase = BUFFERINCREMENT;
            }

            logger.trace("Resizing {}", bufferuse);

            byte[][] temp = new byte[wiredata.length + increase][];
            System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
            wiredata = temp;
        }
    }

    /**
     * Appends a buffer to the buffer list.
     *
     * @param _buf buffer byte array
     */
    protected void appendBytes(byte[] _buf) {
        if (null == _buf) {
            return;
        }
        if (preallocated > 0) {
            if (paofs + _buf.length > pabuf.length) {
                throw new ArrayIndexOutOfBoundsException(
                        MessageFormat.format("Array index out of bounds, paofs={0}, pabuf.length={1}, buf.length={2}.",
                                paofs, pabuf.length, _buf.length));
            }
            System.arraycopy(_buf, 0, pabuf, paofs, _buf.length);
            paofs += _buf.length;
            preallocated -= _buf.length;
        } else {
            if (bufferuse == wiredata.length) {
                logger.trace("Resizing {}", bufferuse);
                byte[][] temp = new byte[wiredata.length + BUFFERINCREMENT][];
                System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
                wiredata = temp;
            }
            wiredata[bufferuse++] = _buf;
            bytecounter += _buf.length;
        }
    }

    /**
     * Appends a byte to the buffer list.
     *
     * @param _b byte
     */
    protected void appendByte(byte _b) {
        if (preallocated > 0) {
            pabuf[paofs++] = _b;
            preallocated--;
        } else {
            if (bufferuse == wiredata.length) {

                logger.trace("Resizing {}", bufferuse);
                byte[][] temp = new byte[wiredata.length + BUFFERINCREMENT][];
                System.arraycopy(wiredata, 0, temp, 0, wiredata.length);
                wiredata = temp;
            }
            wiredata[bufferuse++] = new byte[] {
                    _b
            };
            bytecounter++;
        }
    }

    /**
     * Demarshalls an integer of a given width from a buffer. Endianness is determined from the format of the message.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _width The byte-width of the int.
     *
     * @return long
     */
    public long demarshallint(byte[] _buf, int _ofs, int _width) {
        return big ? demarshallintBig(_buf, _ofs, _width) : demarshallintLittle(_buf, _ofs, _width);
    }

    /**
     * Demarshalls an integer of a given width from a buffer.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _endian The endianness to use in demarshalling.
     * @param _width The byte-width of the int.
     *
     * @return long
     */
    public static long demarshallint(byte[] _buf, int _ofs, byte _endian, int _width) {
        return _endian == Endian.BIG ? demarshallintBig(_buf, _ofs, _width) : demarshallintLittle(_buf, _ofs, _width);
    }

    /**
     * Demarshalls an integer of a given width from a buffer using big-endian format.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _width The byte-width of the int.
     * @return long
     */
    public static long demarshallintBig(byte[] _buf, int _ofs, int _width) {
        long l = 0;
        for (int i = 0; i < _width; i++) {
            l <<= 8;
            l |= _buf[_ofs + i] & 0xFF;
        }
        return l;
    }

    /**
     * Demarshalls an integer of a given width from a buffer using little-endian format.
     *
     * @param _buf The buffer to demarshall from.
     * @param _ofs The offset to demarshall from.
     * @param _width The byte-width of the int.
     *
     * @return long
     */
    public static long demarshallintLittle(byte[] _buf, int _ofs, int _width) {
        long l = 0;
        for (int i = _width - 1; i >= 0; i--) {
            l <<= 8;
            l |= _buf[_ofs + i] & 0xFF;
        }
        return l;
    }

    /**
     * Marshalls an integer of a given width and appends it to the message. Endianness is determined from the message.
     *
     * @param _l The integer to marshall.
     * @param _width The byte-width of the int.
     */
    public void appendint(long _l, int _width) {
        byte[] buf = new byte[_width];
        marshallint(_l, buf, 0, _width);
        appendBytes(buf);
    }

    /**
     * Marshalls an integer of a given width into a buffer. Endianness is determined from the message.
     *
     * @param _l The integer to marshall.
     * @param _buf The buffer to marshall to.
     * @param _ofs The offset to marshall to.
     * @param _width The byte-width of the int.
     */
    public void marshallint(long _l, byte[] _buf, int _ofs, int _width) {
        if (big) {
            marshallintBig(_l, _buf, _ofs, _width);
        } else {
            marshallintLittle(_l, _buf, _ofs, _width);
        }

        LoggingHelper.logIf(logger.isTraceEnabled(),
                () -> logger.trace("Marshalled int {} to {}", _l, Hexdump.toHex(_buf, _ofs, _width, true)));
    }

    /**
     * Marshalls an integer of a given width into a buffer using big-endian format.
     *
     * @param _l The integer to marshall.
     * @param _buf The buffer to marshall to.
     * @param _ofs The offset to marshall to.
     * @param _width The byte-width of the int.
     */
    public static void marshallintBig(long _l, byte[] _buf, int _ofs, int _width) {
        for (int i = _width - 1; i >= 0; i--) {
            _buf[i + _ofs] = (byte) (_l & 0xFF);
            _l >>= 8;
        }
    }

    /**
     * Marshalls an integer of a given width into a buffer using little-endian format.
     *
     * @param _l The integer to marshall.
     * @param _buf The buffer to demarshall to.
     * @param _ofs The offset to demarshall to.
     * @param _width The byte-width of the int.
     */
    public static void marshallintLittle(long _l, byte[] _buf, int _ofs, int _width) {
        for (int i = 0; i < _width; i++) {
            _buf[i + _ofs] = (byte) (_l & 0xFF);
            _l >>= 8;
        }
    }

    public byte[][] getWireData() {
        return wiredata;
    }

    public List<FileDescriptor> getFiledescriptors(){
        return filedescriptors;
    }

    /**
     * Formats the message in a human-readable format.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append('(');
        sb.append(flags);
        sb.append(',');
        sb.append(serial);
        sb.append(')');
        sb.append(' ');
        sb.append('{');
        sb.append(' ');
        if (headers.isEmpty()) {
            sb.append('}');
        } else {
            for (Map.Entry<Byte, Object> entry : headers.entrySet()) {
                sb.append(getHeaderFieldName(entry.getKey()));
                sb.append('=');
                sb.append('>');
                sb.append(entry.getValue());
                sb.append(',');
                sb.append(' ');
            }
            sb.setCharAt(sb.length() - 2, ' ');
            sb.setCharAt(sb.length() - 1, '}');
        }
        sb.append(' ');
        sb.append('{');
        sb.append(' ');
        Object[] largs = null;
        try {
            largs = getParameters();
        } catch (DBusException dbe) {
            logger.debug("", dbe);
        }
        if (null == largs || 0 == largs.length) {
            sb.append('}');
        } else {
            for (Object o : largs) {
                if (o == null) {
                    sb.append("null");
                } else if (o instanceof Object[]) {
                    sb.append(Arrays.deepToString((Object[]) o));
                } else if (o instanceof byte[]) {
                    sb.append(Arrays.toString((byte[]) o));
                } else if (o instanceof int[]) {
                    sb.append(Arrays.toString((int[]) o));
                } else if (o instanceof short[]) {
                    sb.append(Arrays.toString((short[]) o));
                } else if (o instanceof long[]) {
                    sb.append(Arrays.toString((long[]) o));
                } else if (o instanceof boolean[]) {
                    sb.append(Arrays.toString((boolean[]) o));
                } else if (o instanceof double[]) {
                    sb.append(Arrays.toString((double[]) o));
                } else if (o instanceof float[]) {
                    sb.append(Arrays.toString((float[]) o));
                } else {
                    sb.append(o);
                }
                sb.append(',');
                sb.append(' ');
            }
            sb.setCharAt(sb.length() - 2, ' ');
            sb.setCharAt(sb.length() - 1, '}');
        }
        return sb.toString();
    }

    /**
     * Returns the value of the header field of a given field.
     *
     * @param _type The field to return.
     * @return The value of the field or null if unset.
     */
    public Object getHeader(byte _type) {
        return headers.get(_type);
    }

    /**
     * Appends a value to the message. The type of the value is read from a D-Bus signature and used to marshall the
     * value.
     *
     * @param _sigb A buffer of the D-Bus signature.
     * @param _sigofs The offset into the signature corresponding to this value.
     * @param _data The value to marshall.
     * @return The offset into the signature of the end of this value's type.
     */
    @SuppressWarnings("unchecked")
    private int appendOne(byte[] _sigb, int _sigofs, Object _data) throws DBusException {
        try {
            int i = _sigofs;
            logger.trace("{}", bytecounter);
            logger.trace("Appending type: {} value: {}", (char) _sigb[i], _data);

            // pad to the alignment of this type.
            pad(_sigb[i]);
            switch (_sigb[i]) {
            case ArgumentType.BYTE:
                appendByte(((Number) _data).byteValue());
                break;
            case ArgumentType.BOOLEAN:
                appendint(((Boolean) _data).booleanValue() ? 1 : 0, 4);
                break;
            case ArgumentType.DOUBLE:
                long l = Double.doubleToLongBits(((Number) _data).doubleValue());
                appendint(l, 8);
                break;
            case ArgumentType.FLOAT:
                int rf = Float.floatToIntBits(((Number) _data).floatValue());
                appendint(rf, 4);
                break;
            case ArgumentType.UINT32:
                appendint(((Number) _data).longValue(), 4);
                break;
            case ArgumentType.INT64:
                appendint(((Number) _data).longValue(), 8);
                break;
            case ArgumentType.UINT64:
                if (big) {
                    appendint(((UInt64) _data).top(), 4);
                    appendint(((UInt64) _data).bottom(), 4);
                } else {
                    appendint(((UInt64) _data).bottom(), 4);
                    appendint(((UInt64) _data).top(), 4);
                }
                break;
            case ArgumentType.INT32:
                appendint(((Number) _data).intValue(), 4);
                break;
            case ArgumentType.UINT16:
                appendint(((Number) _data).intValue(), 2);
                break;
            case ArgumentType.INT16:
                appendint(((Number) _data).shortValue(), 2);
                break;
            case ArgumentType.FILEDESCRIPTOR:
                filedescriptors.add((FileDescriptor)_data);
                appendint(filedescriptors.size() - 1, 4);
                logger.debug( "Just inserted {} as filedescriptor", filedescriptors.size() - 1 );
                break;
            case ArgumentType.STRING:
            case ArgumentType.OBJECT_PATH:

                String payload;
                // if the given data is an object, not a ObjectPath itself
                if (_data instanceof DBusInterface) {
                    payload = ((DBusInterface) _data).getObjectPath();
                } else {
                    // Strings are marshalled as a UInt32 with the length,
                    // followed by the String, followed by a null byte.
                    payload = _data.toString();
                }

                byte[] payloadbytes = null;
                try {
                    payloadbytes = payload.getBytes("UTF-8");
                } catch (UnsupportedEncodingException uee) {
                    logger.debug("System does not support UTF-8 encoding", uee);
                    throw new DBusException("System does not support UTF-8 encoding");
                }
                logger.trace("Appending String of length {}", payloadbytes.length);
                appendint(payloadbytes.length, 4);
                appendBytes(payloadbytes);
                appendBytes(padding[1]);
                // pad(ArgumentType.STRING);? do we need this?
                break;
            case ArgumentType.SIGNATURE:
                // Signatures are marshalled as a byte with the length,
                // followed by the String, followed by a null byte.
                // Signatures are generally short, so preallocate the array
                // for the string, length and null byte.
                if (_data instanceof Type[]) {
                    payload = Marshalling.getDBusType((Type[]) _data);
                } else {
                    payload = (String) _data;
                }
                byte[] pbytes = payload.getBytes();
                preallocate(2 + pbytes.length);
                appendByte((byte) pbytes.length);
                appendBytes(pbytes);
                appendByte((byte) 0);
                break;
            case ArgumentType.ARRAY:
                // Arrays are given as a UInt32 for the length in bytes,
                // padding to the element alignment, then elements in
                // order. The length is the length from the end of the
                // initial padding to the end of the last element.
                if (logger.isTraceEnabled() && _data instanceof Object[]) {
                    logger.trace("Appending array: {}", Arrays.deepToString((Object[]) _data));
                }

                byte[] alen = new byte[4];
                appendBytes(alen);
                pad(_sigb[++i]);
                long c = bytecounter;

                // optimise primitives
                if (_data.getClass().isArray() && _data.getClass().getComponentType().isPrimitive()) {
                    byte[] primbuf;
                    int algn = getAlignment(_sigb[i]);
                    int len = Array.getLength(_data);
                    switch (_sigb[i]) {
                    case ArgumentType.BYTE:
                        primbuf = (byte[]) _data;
                        break;
                    case ArgumentType.INT16:
                    case ArgumentType.INT32:
                    case ArgumentType.INT64:
                        primbuf = new byte[len * algn];
                        for (int j = 0, k = 0; j < len; j++, k += algn) {
                            marshallint(Array.getLong(_data, j), primbuf, k, algn);
                        }
                        break;
                    case ArgumentType.BOOLEAN:
                        primbuf = new byte[len * algn];
                        for (int j = 0, k = 0; j < len; j++, k += algn) {
                            marshallint(Array.getBoolean(_data, j) ? 1 : 0, primbuf, k, algn);
                        }
                        break;
                    case ArgumentType.DOUBLE:
                        primbuf = new byte[len * algn];
                        if (_data instanceof float[]) {
                            for (int j = 0, k = 0; j < len; j++, k += algn) {
                                marshallint(Double.doubleToRawLongBits(((float[]) _data)[j]), primbuf, k, algn);
                            }
                        } else {
                            for (int j = 0, k = 0; j < len; j++, k += algn) {
                                marshallint(Double.doubleToRawLongBits(((double[]) _data)[j]), primbuf, k, algn);
                            }
                        }
                        break;
                    case ArgumentType.FLOAT:
                        primbuf = new byte[len * algn];
                        for (int j = 0, k = 0; j < len; j++, k += algn) {
                            marshallint(Float.floatToRawIntBits(((float[]) _data)[j]), primbuf, k, algn);
                        }
                        break;
                    default:
                        throw new MarshallingException("Primitive array being sent as non-primitive array.");
                    }
                    appendBytes(primbuf);
                } else if (_data instanceof List) {
                    Object[] contents = ((List<?>) _data).toArray();
                    int diff = i;
                    ensureBuffers(contents.length * 4);
                    for (Object o : contents) {
                        diff = appendOne(_sigb, i, o);
                    }
                    if (contents.length == 0) {
                        diff = EmptyCollectionHelper.determineSignatureOffsetArray(_sigb, diff);
                    }
                    i = diff;
                } else if (_data instanceof Map) {
                    int diff = i;
                    Map<Object, Object> map = (Map<Object, Object>) _data;
                    ensureBuffers(map.size() * 6);
                    for (Map.Entry<Object, Object> o : map.entrySet()) {
                        diff = appendOne(_sigb, i, o);
                    }
                    if (map.isEmpty()) {
                        diff = EmptyCollectionHelper.determineSignatureOffsetDict(_sigb, diff);
                    }
                    i = diff;
                } else {
                    Object[] contents = (Object[]) _data;
                    ensureBuffers(contents.length * 4);
                    int diff = i;
                    for (Object o : contents) {
                        diff = appendOne(_sigb, i, o);
                    }
                    if (contents.length == 0) {
                        diff = EmptyCollectionHelper.determineSignatureOffsetArray(_sigb, diff);
                    }
                    i = diff;
                }
                logger.trace("start: {} end: {} length: {}", c, bytecounter, bytecounter - c);
                marshallint(bytecounter - c, alen, 0, 4);
                break;
            case ArgumentType.STRUCT1:
                // Structs are aligned to 8 bytes
                // and simply contain each element marshalled in order
                Object[] contents;
                if (_data instanceof Container) {
                    contents = ((Container) _data).getParameters();
                } else {
                    contents = (Object[]) _data;
                }
                ensureBuffers(contents.length * 4);
                int j = 0;
                for (i++; _sigb[i] != ArgumentType.STRUCT2; i++) {
                    i = appendOne(_sigb, i, contents[j++]);
                }
                break;
            case ArgumentType.DICT_ENTRY1:
                // Dict entries are the same as structs.
                if (_data instanceof Map.Entry) {
                    i++;
                    i = appendOne(_sigb, i, ((Map.Entry<?, ?>) _data).getKey());
                    i++;
                    i = appendOne(_sigb, i, ((Map.Entry<?, ?>) _data).getValue());
                    i++;
                } else {
                    contents = (Object[]) _data;
                    j = 0;
                    for (i++; _sigb[i] != ArgumentType.DICT_ENTRY2; i++) {
                        i = appendOne(_sigb, i, contents[j++]);
                    }
                }
                break;
            case ArgumentType.VARIANT:
                // Variants are marshalled as a signature
                // followed by the value.
                if (_data instanceof Variant) {
                    Variant<?> var = (Variant<?>) _data;
                    appendOne(new byte[] {
                            ArgumentType.SIGNATURE
                    }, 0, var.getSig());
                    appendOne(var.getSig().getBytes(), 0, var.getValue());
                } else if (_data instanceof Object[]) {
                    contents = (Object[]) _data;
                    appendOne(new byte[] {
                            ArgumentType.SIGNATURE
                    }, 0, contents[0]);
                    appendOne(((String) contents[0]).getBytes(), 0, contents[1]);
                } else {
                    String sig = Marshalling.getDBusType(_data.getClass())[0];
                    appendOne(new byte[] {
                            ArgumentType.SIGNATURE
                    }, 0, sig);
                    appendOne(sig.getBytes(), 0, _data);
                }
                break;
            }
            return i;
        } catch (ClassCastException cce) {
            logger.debug("Trying to marshall to unconvertible type.", cce);
            throw new MarshallingException(
                    MessageFormat.format("Trying to marshall to unconvertible type (from {0} to {1}).",
                            _data.getClass().getName(), (char) _sigb[_sigofs]));
        }
    }

    /**
     * Pad the message to the proper alignment for the given type.
     *
     * @param _type type
     */
    public void pad(byte _type) {
        logger.trace("padding for {}", (char) _type);
        int a = getAlignment(_type);
        logger.trace("{} {} {} {}", preallocated, paofs, bytecounter, a);
        int b = (int) ((bytecounter - preallocated) % a);
        if (0 == b) {
            return;
        }
        a = a - b;
        if (preallocated > 0) {
            paofs += a;
            preallocated -= a;
        } else {
            appendBytes(padding[a]);
        }
        logger.trace("{} {} {} {}", preallocated, paofs, bytecounter, a);

    }

    /**
     * Return the alignment for a given type.
     *
     * @param _type type
     * @return int
     */
    public static int getAlignment(byte _type) {
        switch (_type) {
        case 2:
        case ArgumentType.INT16:
        case ArgumentType.UINT16:
            return 2;
        case 4:
        case ArgumentType.BOOLEAN:
        case ArgumentType.FLOAT:
        case ArgumentType.INT32:
        case ArgumentType.UINT32:
        case ArgumentType.FILEDESCRIPTOR:
        case ArgumentType.STRING:
        case ArgumentType.OBJECT_PATH:
        case ArgumentType.ARRAY:
            return 4;
        case 8:
        case ArgumentType.INT64:
        case ArgumentType.UINT64:
        case ArgumentType.DOUBLE:
        case ArgumentType.STRUCT:
        case ArgumentType.DICT_ENTRY:
        case ArgumentType.STRUCT1:
        case ArgumentType.DICT_ENTRY1:
        case ArgumentType.STRUCT2:
        case ArgumentType.DICT_ENTRY2:
            return 8;
        case 1:
        case ArgumentType.BYTE:
        case ArgumentType.SIGNATURE:
        case ArgumentType.VARIANT:
        default:
            return 1;
        }
    }

    /**
     * Append a series of values to the message.
     *
     * @param _sig The signature(s) of the value(s).
     * @param _data The value(s).
     *
     * @throws DBusException on error
     */
    public void append(String _sig, Object... _data) throws DBusException {
        logger.debug("Appending sig: {} data: {}", _sig, LoggingHelper.arraysDeepString(logger.isDebugEnabled(),_data));
        byte[] sigb = _sig.getBytes();
        int j = 0;
        for (int i = 0; i < sigb.length; i++) {
            logger.trace("Appending item: {} {} {}", i, (char) sigb[i], j);
            i = appendOne(sigb, i, _data[j++]);
        }
    }

    /**
     * Align a counter to the given type.
     *
     * @param _current The current counter.
     * @param _type The type to align to.
     * @return The new, aligned, counter.
     */
    public int align(int _current, byte _type) {
        logger.trace("aligning to {}", (char) _type);
        int a = getAlignment(_type);
        if (0 == _current % a) {
            return _current;
        }
        return _current + (a - (_current % a));
    }

    /**
     * Demarshall one value from a buffer.
     *
     * @param _signatureBuf A buffer of the D-Bus signature.
     * @param _dataBuf The buffer to demarshall from.
     * @param _offsets An array of two ints, which holds the position of the current signature offset and the current
     *            offset of the data buffer.
     * @param _contained converts nested arrays to Lists
     * @return The demarshalled value.
     */
    private Object extractOne(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, boolean _contained)
            throws DBusException {

        logger.trace("Extracting type: {} from offset {}", (char) _signatureBuf[_offsets[OFFSET_SIG]],
                _offsets[OFFSET_DATA]);

        Object rv = null;
        _offsets[OFFSET_DATA] = align(_offsets[OFFSET_DATA], _signatureBuf[_offsets[OFFSET_SIG]]);
        switch (_signatureBuf[_offsets[OFFSET_SIG]]) {
        case ArgumentType.BYTE:
            rv = _dataBuf[_offsets[OFFSET_DATA]++];
            break;
        case ArgumentType.UINT32:
            rv = new UInt32(demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4));
            _offsets[OFFSET_DATA] += 4;
            break;
        case ArgumentType.INT32:
            rv = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            _offsets[OFFSET_DATA] += 4;
            break;
        case ArgumentType.INT16:
            rv = (short) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 2);
            _offsets[OFFSET_DATA] += 2;
            break;
        case ArgumentType.UINT16:
            rv = new UInt16((int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 2));
            _offsets[OFFSET_DATA] += 2;
            break;
        case ArgumentType.INT64:
            rv = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 8);
            _offsets[OFFSET_DATA] += 8;
            break;
        case ArgumentType.UINT64:
            long top;
            long bottom;
            if (big) {
                top = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                bottom = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            } else {
                bottom = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
                _offsets[OFFSET_DATA] += 4;
                top = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            }
            rv = new UInt64(top, bottom);
            _offsets[OFFSET_DATA] += 4;
            break;
        case ArgumentType.DOUBLE:
            long l = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 8);
            _offsets[OFFSET_DATA] += 8;
            rv = Double.longBitsToDouble(l);
            break;
        case ArgumentType.FLOAT:
            int rf = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            _offsets[OFFSET_DATA] += 4;
            rv = Float.intBitsToFloat(rf);
            break;
        case ArgumentType.BOOLEAN:
            rf = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            _offsets[OFFSET_DATA] += 4;
            rv = (1 == rf) ? Boolean.TRUE : Boolean.FALSE;
            break;
        case ArgumentType.ARRAY:
            long size = demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);

            logger.trace("Reading array of size: {}", size);
            _offsets[OFFSET_DATA] += 4;
            byte algn = (byte) getAlignment(_signatureBuf[++_offsets[OFFSET_SIG]]);
            _offsets[OFFSET_DATA] = align(_offsets[OFFSET_DATA], _signatureBuf[_offsets[OFFSET_SIG]]);
            int length = (int) (size / algn);
            if (length > AbstractConnection.MAX_ARRAY_LENGTH) {
                throw new MarshallingException("Arrays must not exceed " + AbstractConnection.MAX_ARRAY_LENGTH);
            }

            rv = optimizePrimitives(_signatureBuf, _dataBuf, _offsets, size, algn, length);

            if (_contained && !(rv instanceof List) && !(rv instanceof Map)) {
                rv = ArrayFrob.listify(rv);
            }
            break;
        case ArgumentType.STRUCT1:
            List<Object> contents = new ArrayList<>();
            while (_signatureBuf[++_offsets[OFFSET_SIG]] != ArgumentType.STRUCT2) {
                contents.add(extractOne(_signatureBuf, _dataBuf, _offsets, true));
            }
            rv = contents.toArray();
            break;
        case ArgumentType.DICT_ENTRY1:
            Object[] decontents = new Object[2];

            LoggingHelper.logIf(logger.isTraceEnabled(), () -> {
                logger.trace("Extracting Dict Entry ({}) from: {}",
                        Hexdump.toAscii(_signatureBuf, _offsets[OFFSET_SIG], _signatureBuf.length - _offsets[OFFSET_SIG]),
                        Hexdump.toHex(_dataBuf, _offsets[OFFSET_DATA], _dataBuf.length - _offsets[OFFSET_DATA], true));
            });

            _offsets[OFFSET_SIG]++;
            decontents[0] = extractOne(_signatureBuf, _dataBuf, _offsets, true);
            _offsets[OFFSET_SIG]++;
            decontents[1] = extractOne(_signatureBuf, _dataBuf, _offsets, true);
            _offsets[OFFSET_SIG]++;
            rv = decontents;
            break;
        case ArgumentType.VARIANT:
            int[] newofs = new int[] {
                    0, _offsets[OFFSET_DATA]
            };
            String sig = (String) extract(ArgumentType.SIGNATURE_STRING, _dataBuf, newofs)[0];
            newofs[OFFSET_SIG] = 0;
            rv = new Variant<>(extract(sig, _dataBuf, newofs)[0], sig);
            _offsets[OFFSET_DATA] = newofs[OFFSET_DATA];
            break;
        case ArgumentType.FILEDESCRIPTOR:
            rv = filedescriptors.get((int)demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4));
            _offsets[OFFSET_DATA] += 4;
            break;
        case ArgumentType.STRING:
            length = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            _offsets[OFFSET_DATA] += 4;
            try {
                rv = new String(_dataBuf, _offsets[OFFSET_DATA], length, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                logger.debug("System does not support UTF-8 encoding", uee);
                throw new DBusException("System does not support UTF-8 encoding");
            }
            _offsets[OFFSET_DATA] += length + 1;
            break;
        case ArgumentType.OBJECT_PATH:
            length = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], 4);
            _offsets[OFFSET_DATA] += 4;
            rv = new ObjectPath(getSource(), new String(_dataBuf, _offsets[OFFSET_DATA], length));
            _offsets[OFFSET_DATA] += length + 1;
            break;
        case ArgumentType.SIGNATURE:
            length = _dataBuf[_offsets[OFFSET_DATA]++] & 0xFF;
            rv = new String(_dataBuf, _offsets[OFFSET_DATA], length);
            _offsets[OFFSET_DATA] += length + 1;
            break;
        default:
            throw new UnknownTypeCodeException(_signatureBuf[_offsets[OFFSET_SIG]]);
        }
        if (logger.isTraceEnabled()) {
            if (rv instanceof Object[]) {
                logger.trace("Extracted: {} (now at {})", Arrays.deepToString((Object[]) rv), _offsets[OFFSET_DATA]);
            } else {
                logger.trace("Extracted: {} (now at {})", rv, _offsets[OFFSET_DATA]);
            }
        }
        return rv;
    }

    private Object optimizePrimitives(byte[] _signatureBuf, byte[] _dataBuf, int[] _offsets, long _size, byte _algn,
            int _length)
            throws DBusException {
        Object rv;
        switch (_signatureBuf[_offsets[OFFSET_SIG]]) {
        case ArgumentType.BYTE:
            rv = new byte[_length];
            System.arraycopy(_dataBuf, _offsets[OFFSET_DATA], rv, 0, _length);
            _offsets[OFFSET_DATA] += _size;
            break;
        case ArgumentType.INT16:
            rv = new short[_length];
            for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                ((short[]) rv)[j] = (short) demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
            }
            break;
        case ArgumentType.INT32:
            rv = new int[_length];
            for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                ((int[]) rv)[j] = (int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
            }
            break;
        case ArgumentType.INT64:
            rv = new long[_length];
            for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                ((long[]) rv)[j] = demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
            }
            break;
        case ArgumentType.BOOLEAN:
            rv = new boolean[_length];
            for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                ((boolean[]) rv)[j] = 1 == demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn);
            }
            break;
        case ArgumentType.FLOAT:
            rv = new float[_length];
            for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                ((float[]) rv)[j] = Float.intBitsToFloat((int) demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn));
            }
            break;
        case ArgumentType.DOUBLE:
            rv = new double[_length];
            for (int j = 0; j < _length; j++, _offsets[OFFSET_DATA] += _algn) {
                ((double[]) rv)[j] = Double.longBitsToDouble(demarshallint(_dataBuf, _offsets[OFFSET_DATA], _algn));
            }
            break;
        case ArgumentType.DICT_ENTRY1:
            int ofssave = prepareCollection(_signatureBuf, _offsets, _size);
            long end = _offsets[OFFSET_DATA] + _size;
            List<Object[]> entries = new ArrayList<>();
            while (_offsets[OFFSET_DATA] < end) {
                _offsets[OFFSET_SIG] = ofssave;
                entries.add((Object[]) extractOne(_signatureBuf, _dataBuf, _offsets, true));
            }
            rv = new DBusMap<>(entries.toArray(new Object[0][]));
            break;
        default:
            ofssave = prepareCollection(_signatureBuf, _offsets, _size);
            end = _offsets[OFFSET_DATA] + _size;
            List<Object> contents = new ArrayList<>();
            while (_offsets[OFFSET_DATA] < end) {
                _offsets[OFFSET_SIG] = ofssave;
                contents.add(extractOne(_signatureBuf, _dataBuf, _offsets, true));
            }
            rv = contents;
        }
        return rv;
    }

    private int prepareCollection(byte[] _signatureBuf, int[] _offsets, long _size) throws DBusException {
        if (0 == _size) {
            // advance the type parser even on 0-size arrays.
            List<Type> temp = new ArrayList<>();
            byte[] temp2 = new byte[_signatureBuf.length - _offsets[OFFSET_SIG]];
            System.arraycopy(_signatureBuf, _offsets[OFFSET_SIG], temp2, 0, temp2.length);
            String temp3 = new String(temp2);
            // ofs[OFFSET_SIG] gets incremented anyway. Leave one character on the stack
            int temp4 = Marshalling.getJavaType(temp3, temp, 1) - 1;
            _offsets[OFFSET_SIG] += temp4;
            logger.trace("Aligned type: {} {} {}", temp3, temp4, _offsets[OFFSET_SIG]);
        }
        int ofssave = _offsets[OFFSET_SIG];
        return ofssave;
    }

    /**
     * Demarshall values from a buffer.
     *
     * @param _signature The D-Bus signature(s) of the value(s).
     * @param _dataBuf The buffer to demarshall from.
     * @param _offsets The offset into the data buffer to start.
     * @return The demarshalled value(s).
     *
     * @throws DBusException on error
     */
    public Object[] extract(String _signature, byte[] _dataBuf, int _offsets) throws DBusException {
        return extract(_signature, _dataBuf, new int[] {
                0, _offsets
        });
    }

    /**
     * Demarshall values from a buffer.
     *
     * @param _signature The D-Bus signature(s) of the value(s).
     * @param _dataBuf The buffer to demarshall from.
     * @param _offsets An array of two ints, which holds the position of the current signature offset and the current
     *            offset of the data buffer. These values will be updated to the start of the next value after
     *            demarshalling.
     * @return The demarshalled value(s).
     *
     * @throws DBusException on error
     */
    public Object[] extract(String _signature, byte[] _dataBuf, int[] _offsets) throws DBusException {
        logger.trace("extract({},#{}, {{},{}}", _signature, _dataBuf.length, _offsets[OFFSET_SIG],
                _offsets[OFFSET_DATA]);
        List<Object> rv = new ArrayList<>();
        byte[] sigb = _signature.getBytes();
        for (int[] i = _offsets; i[0] < sigb.length; i[0]++) {
            rv.add(extractOne(sigb, _dataBuf, i, false));
        }
        return rv.toArray();
    }

    /**
     * Returns the Bus ID that sent the message.
     *
     * @return string
     */
    public String getSource() {
        return (String) headers.get(HeaderField.SENDER);
    }

    /**
     * Returns the destination of the message.
     *
     * @return string
     */
    public String getDestination() {
        return (String) headers.get(HeaderField.DESTINATION);
    }

    /**
     * Returns the interface of the message.
     *
     * @return string
     */
    public String getInterface() {
        return (String) headers.get(HeaderField.INTERFACE);
    }

    /**
     * Returns the object path of the message.
     *
     * @return string
     */
    public String getPath() {
        Object o = headers.get(HeaderField.PATH);
        if (null == o) {
            return null;
        }
        return o.toString();
    }

    /**
     * Returns the member name or error name this message represents.
     *
     * @return string
     */
    public String getName() {
        if (this instanceof org.freedesktop.dbus.errors.Error) {
            return (String) headers.get(HeaderField.ERROR_NAME);
        } else {
            return (String) headers.get(HeaderField.MEMBER);
        }
    }

    /**
     * Returns the dbus signature of the parameters.
     *
     * @return string
     */
    public String getSig() {
        return (String) headers.get(HeaderField.SIGNATURE);
    }

    /**
     * Returns the message flags.
     *
     * @return int
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns the message serial ID (unique for this connection)
     *
     * @return the message serial.
     */
    public long getSerial() {
        return serial;
    }

    /**
     * If this is a reply to a message, this returns its serial.
     *
     * @return The reply serial, or 0 if it is not a reply.
     */
    public long getReplySerial() {
        Number l = (Number) headers.get(HeaderField.REPLY_SERIAL);
        if (null == l) {
            return 0;
        }
        return l.longValue();
    }

    /**
     * Parses and returns the parameters to this message as an Object array.
     *
     * @return object array
     * @throws DBusException on failure
     */
    public Object[] getParameters() throws DBusException {
        if (null == args && null != body) {
            String sig = (String) headers.get(HeaderField.SIGNATURE);
            if (null != sig && 0 != body.length) {
                args = extract(sig, body, 0);
            } else {
                args = new Object[0];
            }
        }
        return args;
    }

    public void setArgs(Object[] _args) {
        this.args = _args;
    }

    /**
     * Warning, do not use this method unless you really know what you are doing.
     *
     * @param _source string
     * @throws DBusException on error
     */
    public void setSource(String _source) throws DBusException {
        if (null != body) {
            wiredata = new byte[BUFFERINCREMENT][];
            bufferuse = 0;
            bytecounter = 0;
            preallocate(12);
            append("yyyyuu", big ? Endian.BIG : Endian.LITTLE, type, flags, protover, bodylen, serial);
            headers.put(HeaderField.SENDER, _source);
            Object[][] newhead = new Object[headers.size()][];
            int i = 0;
            for (Byte b : headers.keySet()) {
                newhead[i] = new Object[2];
                newhead[i][0] = b;
                newhead[i][1] = headers.get(b);
                i++;
            }
            append("a(yv)", (Object) newhead);
            pad((byte) 8);
            appendBytes(body);
        }
    }

    /**
     * Type of this message.
     * @return byte
     */
    public byte getType() {
        return type;
    }

    public byte getEndianess() {
        return big ? Endian.BIG : Endian.LITTLE;
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
    protected Object[] createHeaderArgs(byte _header, String _argType, Object _value) {
        getHeaders().put(_header, _value);
        return new Object[] {
                _header, new Object[] {
                        _argType, _value
                }
        };
    }

    /**
     * Adds message padding and marshalling.
     *
     * @param _hargs
     * @param _serial
     * @param _sig
     * @param _args
     * @throws DBusException
     */
    protected void padAndMarshall(List<Object> _hargs, long _serial, String _sig, Object... _args) throws DBusException {
        byte[] blen = new byte[4];
        appendBytes(blen);
        append("ua(yv)", _serial, _hargs.toArray());
        pad((byte) 8);

        long c = getByteCounter();
        if (null != _sig) {
            append(_sig, _args);
        }
        logger.trace("Appended body, type: {} start: {} end: {} size: {}",_sig, c, getByteCounter(), getByteCounter() - c);
        marshallint(getByteCounter() - c, blen, 0, 4);
        logger.trace("marshalled size ({}): {}",blen, Hexdump.format(blen));
    }

    /**
     * Returns the name of the given header field.
     *
     * @param _field field
     * @return string
     */
    public static String getHeaderFieldName(byte _field) {
        switch (_field) {
        case HeaderField.PATH:
            return "Path";
        case HeaderField.INTERFACE:
            return "Interface";
        case HeaderField.MEMBER:
            return "Member";
        case HeaderField.ERROR_NAME:
            return "Error Name";
        case HeaderField.REPLY_SERIAL:
            return "Reply Serial";
        case HeaderField.DESTINATION:
            return "Destination";
        case HeaderField.SENDER:
            return "Sender";
        case HeaderField.SIGNATURE:
            return "Signature";
        case HeaderField.UNIX_FDS:
            return "Unix FD";
        default:
            return "Invalid";
        }
    }

    /** Defines constants representing the flags which can be set on a message. */
    public interface Flags {
        byte NO_REPLY_EXPECTED = 0x01;
        byte NO_AUTO_START     = 0x02;
        byte ASYNC             = 0x40;
    }

    /** Defines constants for each message type. */
    public interface MessageType {
        byte METHOD_CALL   = 1;
        byte METHOD_RETURN = 2;
        byte ERROR         = 3;
        byte SIGNAL        = 4;
    }

    /** Defines constants for each valid header field type. */
    public interface HeaderField {
        byte PATH         = 1;
        byte INTERFACE    = 2;
        byte MEMBER       = 3;
        byte ERROR_NAME   = 4;
        byte REPLY_SERIAL = 5;
        byte DESTINATION  = 6;
        byte SENDER       = 7;
        byte SIGNATURE    = 8;
        byte UNIX_FDS     = 9;
    }

    /**
     * Defines constants for each argument type. There are two constants for each argument type, as a byte or as a
     * String (the _STRING version)
     */
    public interface ArgumentType {
        String BYTE_STRING           = "y";
        String BOOLEAN_STRING        = "b";
        String INT16_STRING          = "n";
        String UINT16_STRING         = "q";
        String INT32_STRING          = "i";
        String UINT32_STRING         = "u";
        String INT64_STRING          = "x";
        String UINT64_STRING         = "t";
        String DOUBLE_STRING         = "d";
        String FLOAT_STRING          = "f";
        String STRING_STRING         = "s";
        String OBJECT_PATH_STRING    = "o";
        String SIGNATURE_STRING      = "g";
        String FILEDESCRIPTOR_STRING = "h";
        String ARRAY_STRING          = "a";
        String VARIANT_STRING        = "v";
        String STRUCT_STRING         = "r";
        String STRUCT1_STRING        = "(";
        String STRUCT2_STRING        = ")";
        String DICT_ENTRY_STRING     = "e";
        String DICT_ENTRY1_STRING    = "{";
        String DICT_ENTRY2_STRING    = "}";

        byte   BYTE                  = 'y';
        byte   BOOLEAN               = 'b';
        byte   INT16                 = 'n';
        byte   UINT16                = 'q';
        byte   INT32                 = 'i';
        byte   UINT32                = 'u';
        byte   INT64                 = 'x';
        byte   UINT64                = 't';
        byte   DOUBLE                = 'd';
        byte   FLOAT                 = 'f';
        byte   STRING                = 's';
        byte   OBJECT_PATH           = 'o';
        byte   SIGNATURE             = 'g';
        byte   FILEDESCRIPTOR        = 'h';
        byte   ARRAY                 = 'a';
        byte   VARIANT               = 'v';
        byte   STRUCT                = 'r';
        byte   STRUCT1               = '(';
        byte   STRUCT2               = ')';
        byte   DICT_ENTRY            = 'e';
        byte   DICT_ENTRY1           = '{';
        byte   DICT_ENTRY2           = '}';
    }

    /** Defines constants representing the endianness of the message. */
    public interface Endian {
        byte BIG    = 'B';
        byte LITTLE = 'l';
    }
}
