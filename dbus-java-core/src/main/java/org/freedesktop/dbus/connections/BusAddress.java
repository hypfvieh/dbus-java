package org.freedesktop.dbus.connections;

import org.freedesktop.dbus.exceptions.InvalidBusAddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Defines an address to connect to DBus.
 * The address will define which transport to use.
 */
public class BusAddress {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusAddress.class);
    private static final char[] HEX    = "0123456789ABCDEF".toCharArray();

    private String                    type;
    private final Map<String, String> parameters = new LinkedHashMap<>();

    protected BusAddress(BusAddress _obj) {
        if (_obj != null) {
            parameters.putAll(_obj.parameters);
            type = _obj.type;
        }
    }

    /**
     * Creates a copy of the given {@link BusAddress}.
     * If given address is null, an empty {@link BusAddress} object is created.
     *
     * @param _address address to copy
     * @return BusAddress
     * @since 4.2.0 - 2022-07-18
     */
    public static BusAddress of(BusAddress _address) {
        return new BusAddress(_address);
    }

    /**
     * Creates a new {@link BusAddress} from String.
     *
     * @param _address address String, never null or empty
     *
     * @return BusAddress
     * @since 4.2.0 - 2022-07-18
     */
    public static BusAddress of(String _address) {
        List<BusAddress> all = parseAll(_address);
        if (all.isEmpty()) {
            throw new InvalidBusAddressException("Bus address is invalid: " + _address);
        }
        return all.getFirst();
    }

    /**
     * Parses an address string which may contain multiple {@code ;}-separated addresses (as defined by
     * the D-Bus specification) into a list of {@link BusAddress} objects, in the order given.
     *
     * @param _address address String, never null or empty
     *
     * @return list of BusAddress (never empty)
     * @since 6.0.0 - 2026-07-18
     */
    public static List<BusAddress> parseAll(String _address) {
        if (_address == null || _address.isEmpty()) {
            throw new InvalidBusAddressException("Bus address is blank");
        }

        List<BusAddress> result = new ArrayList<>();
        for (String single : _address.split(";")) {
            if (single.isBlank()) {
                continue;
            }
            result.add(parseSingle(single));
        }

        if (result.isEmpty()) {
            throw new InvalidBusAddressException("Bus address is invalid: " + _address);
        }

        return result;
    }

    private static BusAddress parseSingle(String _address) {
        BusAddress busAddress = new BusAddress(null);

        LOGGER.trace("Parsing bus address: {}", _address);

        String[] ss = _address.split(":", 2);
        if (ss.length < 2) {
            throw new InvalidBusAddressException("Bus address is invalid: " + _address);
        }

        busAddress.type = ss[0] != null ? ss[0].toLowerCase(Locale.US) : null;
        if (busAddress.type == null) {
            throw new InvalidBusAddressException("Unsupported transport type: " + ss[0]);
        }

        LOGGER.trace("Transport type: {}", busAddress.type);

        if (!ss[1].isEmpty()) {
            for (String p : ss[1].split(",")) {
                if (p.isEmpty()) {
                    continue;
                }
                String[] kv = p.split("=", 2);
                busAddress.addParameter(kv[0], kv.length > 1 ? unescapeValue(kv[1]) : "");
            }
        }

        LOGGER.trace("Transport options: {}", busAddress.parameters);

        return busAddress;
    }

    /**
     * Returns the transport type as found in the address.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the transport type in uppercase.
     *
     * @return type
     */
    public String getBusType() {
        return type == null ? null : type.toUpperCase(Locale.US);
    }

    /**
     * Checks if this {@link BusAddress} is for the given bus type.<br>
     * The given type will be compared case-insensitive.
     * <br>
     * e.g.
     * <pre>
     * isBusType("unix");
     * </pre>
     *
     * @param _type to compare
     *
     * @return true if same type (case-insensitive), false if null or not same type
     *
     * @since 4.2.0 - 2022-07-20
     */
    public boolean isBusType(String _type) {
        return type != null && type.equalsIgnoreCase(_type);
    }

    /**
     * True if this is a listening address.
     * @return true if listening
     */
    public boolean isListeningSocket() {
        return parameters.containsKey("listen");
    }

    public String getGuid() {
        return parameters.get("guid");
    }

    @Override
    public final String toString() {
        return type + ":" + parameters.entrySet().stream()
            .map(e -> e.getKey() + "=" + escapeValue(e.getValue()))
            .collect(Collectors.joining(","));
    }

    /**
     * Decodes a {@code %HH}-escaped address parameter value (D-Bus address escaping) back to its raw
     * value. Decoding is byte-based (UTF-8); a {@code %} not followed by two hex digits is left as-is.
     *
     * @param _value escaped value
     * @return decoded value
     */
    private static String unescapeValue(String _value) {
        if (_value.indexOf('%') < 0) {
            return _value; // nothing to unescape
        }

        byte[] raw = _value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
        int i = 0;
        while (i < raw.length) {
            byte b = raw[i];
            if (b == '%' && i + 2 < raw.length && isHex(raw[i + 1]) && isHex(raw[i + 2])) {
                out.write((Character.digit(raw[i + 1], 16) << 4) | Character.digit(raw[i + 2], 16));
                i += 3;
            } else {
                out.write(b);
                i++;
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Escapes an address parameter value according to the D-Bus address escaping rules: every byte which
     * is not in the optionally-escaped set {@code [-0-9A-Za-z_/.*]} is replaced by {@code %HH}.
     *
     * @param _value raw value
     * @return escaped value
     */
    private static String escapeValue(String _value) {
        byte[] raw = _value.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(raw.length);
        for (byte b : raw) {
            int c = b & 0xFF;
            if (isOptionallyEscaped(c)) {
                sb.append((char) c);
            } else {
                sb.append('%').append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
            }
        }
        return sb.toString();
    }

    private static boolean isOptionallyEscaped(int _c) {
        return _c == '-' || _c == '_' || _c == '/' || _c == '.' || _c == '*'
            || _c >= '0' && _c <= '9' || _c >= 'A' && _c <= 'Z' || _c >= 'a' && _c <= 'z';
    }

    private static boolean isHex(byte _b) {
        return _b >= '0' && _b <= '9' || _b >= 'a' && _b <= 'f' || _b >= 'A' && _b <= 'F';
    }

    /**
     * True if this address represents a listening server address.
     * @return true if server
     */
    public boolean isServer() {
        return isListeningSocket();
    }

    /**
     * Add a parameter to the address.
     * Adding multiple parameters with same name is not possible and will overwrite previous values.
     *
     * @param _parameter parameter name
     * @param _value value
     *
     * @return this
     * @since 4.2.0 - 2022-07-18
     */
    public BusAddress addParameter(String _parameter, String _value) {
        parameters.put(_parameter, _value);
        return this;
    }

    /**
     * Remove parameter with given name.
     * If parameter does not exists, nothing will happen.
     *
     * @param _parameter parameter to remove
     *
     * @return this
     * @since 4.2.0 - 2022-07-18
     */
    public BusAddress removeParameter(String _parameter) {
        parameters.remove(_parameter);
        return this;
    }

    /**
     * Checks if the given parameter is present.
     *
     * @param _parameter parameter to check
     *
     * @return true if parameter exists, false otherwise
     * @since 4.2.2 - 2023-01-11
     */
    public boolean hasParameter(String _parameter) {
        return parameters.containsKey(_parameter);
    }

    /**
     * Returns a the value of the given parameter.
     * <p>
     * When no value present, <code>null</code> is returned.
     *
     * @param _parameter parameter to get value for
     *
     * @return String or <code>null</code>
     * @since 4.2.0 - 2022-07-19
     */
    public String getParameterValue(String _parameter) {
        return parameters.get(_parameter);
    }

    /**
     * Returns a the value of the given parameter.
     * <p>
     * When no value present, the given default is returned.
     *
     * @param _parameter parameter to get value for
     * @param _default default to return if parameter not set
     *
     * @return String or default
     * @since 4.2.2 - 2023-01-11
     */
    public String getParameterValue(String _parameter, String _default) {
        return parameters.getOrDefault(_parameter, _default);
    }

    /**
     * Creates a listening BusAddress if this instance is not already listening.
     *
     * @return new BusAddress or this
     * @since 4.2.0 - 2022-07-18
     */
    public BusAddress getListenerAddress() {
        if (!isListeningSocket()) {
            return new BusAddress(this).addParameter("listen", "true");
        }
        return this;
    }

}
