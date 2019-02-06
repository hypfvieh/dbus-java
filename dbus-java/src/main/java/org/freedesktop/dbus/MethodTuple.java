/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodTuple {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private String sig;

    public MethodTuple(String _name, String _sig) {
        this.name = _name;
        if (null != _sig) {
            this.sig = _sig;
        } else {
            this.sig = "";
        }
        logger.trace("new MethodTuple({}, {})", this.name, this.sig);
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(MethodTuple.class) && ((MethodTuple) o).name.equals(this.name) && ((MethodTuple) o).sig.equals(this.sig);
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
