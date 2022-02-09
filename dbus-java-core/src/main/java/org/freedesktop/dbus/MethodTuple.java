package org.freedesktop.dbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodTuple {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name;
    private final String sig;

    public MethodTuple(String _name, String _sig) {
        name = _name;
        if (null != _sig) {
            sig = _sig;
        } else {
            sig = "";
        }
        logger.trace("new MethodTuple({}, {})", name, sig);
    }

    @Override
    public boolean equals(Object _o) {
        return _o.getClass().equals(MethodTuple.class) && ((MethodTuple) _o).name.equals(this.name) && ((MethodTuple) _o).sig.equals(this.sig);
    }

    @Override
    public int hashCode() {
        return name.hashCode() + sig.hashCode();
    }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }

    public String getSig() {
        return sig;
    }
}
