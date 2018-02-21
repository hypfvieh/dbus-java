package org.freedesktop.dbus.connections;

import org.freedesktop.DBus;
import org.freedesktop.dbus.ExportedObject;

public class GlobalHandler implements org.freedesktop.DBus.Peer, org.freedesktop.DBus.Introspectable {
    /**
     * 
     */
    private AbstractConnection connection;
    private String objectpath;

    GlobalHandler(AbstractConnection _abstractConnection) {
        connection = _abstractConnection;
        this.objectpath = null;
    }

    GlobalHandler(AbstractConnection _abstractConnection, String _objectpath) {
        connection = _abstractConnection;
        this.objectpath = _objectpath;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public void Ping() {
        return;
    }

    @Override
    public String Introspect() {
        String intro = connection.objectTree.Introspect(objectpath);
        if (null == intro) {
            ExportedObject eo = connection.fallbackContainer.get(objectpath);
            if (null != eo) {
                intro = eo.getIntrospectiondata();
            }
        }
        if (null == intro) {
            throw new DBus.Error.UnknownObject("Introspecting on non-existant object");
        } else {
            return "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\" "
                    + "\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n" + intro;
        }
    }

    @Override
    public String getObjectPath() {
        return objectpath;
    }
}