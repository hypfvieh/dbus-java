package org.freedesktop.dbus.connections;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusAddress {
    private final Logger        logger = LoggerFactory.getLogger(getClass());

    private final String              type;
    private final Map<String, String> parameters = new HashMap<>();

    private final String rawAddress;

    public BusAddress(String _address) throws DBusException {
        if (null == _address || "".equals(_address)) {
            throw new DBusException("Bus address is blank");
        }

        logger.trace("Parsing bus address: {}", _address);

        String[] ss = _address.split(":", 2);
        if (ss.length < 2) {
            throw new DBusException("Bus address is invalid: " + _address);
        }

        type = ss[0] != null ? ss[0].toLowerCase() : null;
        if (type == null) {
            throw new DBusException("Unsupported transport type: " + ss[0]);
        }

        logger.trace("Transport type: {}", type);

        rawAddress = _address;

        String[] ps = ss[1].split(",");
        for (String p : ps) {
            String[] kv = p.split("=", 2);
            parameters.put(kv[0], kv[1]);
        }

        logger.trace("Transport options: {}", parameters);

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

    public boolean hasGuid() {
        return parameters.containsKey("guid");
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
    public String toString() {
        return type + ": " + parameters;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public boolean isServer() {
        return isListeningSocket();
    }

    public BusAddress getListenerAddress() {
        if (!isListeningSocket()) {
            try {
                return new BusAddress(rawAddress + ",listen=true");
            } catch (DBusException _ex) {
            }
        }
        return this;
    }
}
