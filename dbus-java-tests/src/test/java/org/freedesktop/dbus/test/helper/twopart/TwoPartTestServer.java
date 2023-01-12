package org.freedesktop.dbus.test.helper.twopart;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartInterface;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwoPartTestServer implements TwoPartInterface, DBusSigHandler<TwoPartInterface.TwoPartSignal> {
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final DBusConnection conn;

    public TwoPartTestServer(DBusConnection _conn) {
        this.conn = _conn;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public TwoPartObject getNew() {
        TwoPartObject o = new TwoPartTestObject();
        logger.debug("export new");
        try {
            conn.exportObject("/12345", o);
        } catch (Exception _ex) {
            logger.debug("Caught exception: {}", _ex.getMessage());
        }
        logger.debug("give new");
        return o;
    }

    @Override
    public void handle(TwoPartInterface.TwoPartSignal _s) {
        logger.debug("Got: " + _s.o);
    }

    public class TwoPartTestObject implements TwoPartObject {
        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public String getName() {
            return "give name";
        }
    }

}
