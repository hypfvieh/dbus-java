package org.freedesktop.dbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MethodTuple(String name, String sig) {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodTuple.class);

    public MethodTuple {
        if (sig == null) {
            sig = "";
        }
        LOGGER.trace("new MethodTuple({}, {})", name, sig);
    }
}
