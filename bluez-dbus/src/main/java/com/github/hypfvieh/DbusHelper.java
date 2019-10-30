package com.github.hypfvieh;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Various DBUS related helper methods.
 * @author hypfvieh
 *
 */
public final class DbusHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbusHelper.class);

    private DbusHelper() {

    }

    /**
     * Find all &lt;node&gt;-Elements in DBUS Introspection XML and extracts the value of the 'name' attribute.
     * @param _connection the dbus connection
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
            LOGGER.info("Exception while search DBus.", _ex);
        } catch (IOException _ex) {
            LOGGER.error("Exception while applying Xpath to introspection result", _ex);
        } catch (Exception _ex) {
            LOGGER.error("Critical error while reading DBUS response (maybe no bluetoothd daemon running?)", _ex);
        }
        return foundNodes;
    }

    /**
     * Creates an java object from a bluez dbus response.
     * @param _connection Dbus connection to use
     * @param _path dbus request path
     * @param _objClass interface class to use
     * @param <T> some class/interface implementing/extending {@link DBusInterface}
     * @return the created object or null on error
     */
    public static <T extends DBusInterface> T getRemoteObject(DBusConnection _connection, String _path, Class<T> _objClass) {
        try {
            return _connection.getRemoteObject("org.bluez", _path, _objClass);
        } catch (DBusException _ex) {
            LOGGER.warn("Error while converting dbus response to object.", _ex);
        }
        return null;
    }

}
