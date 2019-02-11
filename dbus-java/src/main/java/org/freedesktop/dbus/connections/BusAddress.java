/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2018 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.connections;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.util.TypeUtil;

public class BusAddress {
    private final Logger        logger = LoggerFactory.getLogger(getClass());

    private final AddressBusTypes     type;
    private final Map<String, String> parameters = new HashMap<>();

    private final String rawAddress;
    
    public BusAddress(String address) throws DBusException {
        if (null == address || "".equals(address)) {
            throw new DBusException("Bus address is blank");
        }

        logger.trace("Parsing bus address: {}", address);

        String[] ss = address.split(":", 2);
        if (ss.length < 2) {
            throw new DBusException("Bus address is invalid: " + address);
        }

        type = AddressBusTypes.toEnum(ss[0]);
        if (type == null) {
            throw new DBusException("Unsupported transport type: " + ss[0]);
        }

        logger.trace("Transport type: {}", type);

        rawAddress = address;
        
        String[] ps = ss[1].split(",");
        for (String p : ps) {
            String[] kv = p.split("=", 2);
            parameters.put(kv[0], kv[1]);
        }

        logger.trace("Transport options: {}", parameters);

    }

    public String getType() {
        return type.getBusType();
    }

    public AddressBusTypes getBusType() {
        return type;
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
        return TypeUtil.isValidNetworkPort(parameters.get("port"), true) ? Integer.parseInt(parameters.get("port")) : null;
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
    
    public static enum AddressBusTypes {
        UNIX,
        TCP;

        public String getBusType() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static AddressBusTypes toEnum(String _str) {
            for (AddressBusTypes itm : values()) {
                if (itm.getBusType().equals(_str.toLowerCase(Locale.ROOT))) {
                    return itm;
                }
            }
            return null;
        }
    }

}
