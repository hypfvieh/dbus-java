package org.freedesktop.dbus.connections;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.freedesktop.dbus.exceptions.InvalidBusAddressException;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusAddress {
    private static final Logger       LOGGER     = LoggerFactory.getLogger(BusAddress.class);

    private String                    type;
    private final Map<String, String> parameters = new LinkedHashMap<>();

    public BusAddress() {
        this(null);
    }

    public BusAddress(BusAddress _obj) {
        if (_obj != null) {
            parameters.putAll(_obj.parameters);
            type = _obj.type;
        }
    }

    public static BusAddress of(String _address) {
        if (null == _address ||_address.isEmpty()) {
            throw new InvalidBusAddressException("Bus address is blank");
        }

        BusAddress busAddress = new BusAddress(null);

        LOGGER.trace("Parsing bus address: {}", _address);

        String[] ss = _address.split(":", 2);
        if (ss.length < 2) {
            throw new InvalidBusAddressException("Bus address is invalid: " + _address);
        }

        busAddress.type = ss[0] != null ? ss[0].toLowerCase() : null;
        if (busAddress.type == null) {
            throw new InvalidBusAddressException("Unsupported transport type: " + ss[0]);
        }

        LOGGER.trace("Transport type: {}", busAddress.type);

        String[] ps = ss[1].split(",");
        for (String p : ps) {
            String[] kv = p.split("=", 2);
            busAddress.addParameter(kv[0], kv[1]);
        }

        LOGGER.trace("Transport options: {}", busAddress.parameters);

        return busAddress;
    }

    public String getType() {
        return type;
    }

    public String getBusType() {
        return type.toUpperCase();
    }

    public boolean isFileBasedAddress() {
        return !isAbstract() && !hasHost() && !hasPort();
    }

    public boolean isAbstract() {
        return parameters.containsKey("abstract");
    }

    public boolean isListeningSocket() {
        return parameters.containsKey("listen");
    }

    public boolean hasPath() {
        return parameters.containsKey("path");
    }

    public boolean hasHost() {
        return parameters.containsKey("host");
    }

    public boolean hasPort() {
        return parameters.containsKey("port");
    }

    public String getAbstract() {
        return parameters.get("abstract");
    }

    public String getPath() {
        return parameters.get("path");
    }

    public int getPort() {
        return Util.isValidNetworkPort(parameters.get("port"), true) ? Integer.parseInt(parameters.get("port")) : null;
    }

    public String getHost() {
        return parameters.get("host");
    }

    public String getGuid() {
        return parameters.get("guid");
    }

    @Override
    public final String toString() {
        return type + ":" + parameters.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
    }

    public boolean isServer() {
        return isListeningSocket();
    }

    public BusAddress addParameter(String _parameter, String _value) {
        parameters.put(_parameter, _value);
        return this;
    }

    public BusAddress removeParameter(String _parameter) {
        parameters.remove(_parameter);
        return this;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public BusAddress getListenerAddress() {
        if (!isListeningSocket()) {
            return new BusAddress(this).addParameter("listen", "true");
        }
        return this;
    }

}
