package org.caseof;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.freedesktop.DBus.Introspectable;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.exceptions.DBusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Various DBUS related helper methods.
 * @author maniac
 *
 */
public final class DbusHelper {

    private DbusHelper() {

    }

    /**
     * Find all &lt;node&gt;-Elements in DBUS Introspection XML and extracts the value of the 'name' attribute.
     * @param _path dbus-path-to-introspect
     * @return Set of String, maybe empty but never null
     */
    public static Set<String> findNodes(DBusConnection _connection, String _path) {
        Set<String> foundNodes = new LinkedHashSet<>();
        if (_connection == null || StringUtils.isBlank(_path)) {
            return foundNodes;
        }
        try {
            Introspectable remoteObject = _connection.getRemoteObject("org.bluez", _path, Introspectable.class);
            String introspect = remoteObject.Introspect();
            Document doc = XmlHelper.parseXmlString(introspect);
            NodeList nodes = XmlHelper.applyXpathExpressionToDocument("/node/node", doc);
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element) {
                    Element elem = (Element) nodes.item(i);
                    foundNodes.add(elem.getAttribute("name"));
                }
            }
            return foundNodes;
        } catch (DBusException _ex) {
            // TODO Auto-generated catch block
            _ex.printStackTrace();
        } catch (IOException _ex) {
            // TODO Auto-generated catch block
            _ex.printStackTrace();
        }
        return foundNodes;
    }

    public static <T extends DBusInterface> T getRemoteObject(DBusConnection _connection, String _path, Class<T> _objClass) {
        try {
            return _connection.getRemoteObject("org.bluez", _path, _objClass);
        } catch (DBusException _ex) {
            // TODO Auto-generated catch block
            _ex.printStackTrace();
        }
        return null;
    }

}
