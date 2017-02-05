package org.caseof.bluetooth.wrapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.ClassUtils;
import org.bluez.Adapter1;
import org.caseof.DbusHelper;
import org.freedesktop.DBus.Properties;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * Base class of all bluetooth wrapper object classes.
 *
 * @author maniac
 *
 */
public abstract class AbstractBluetoothObject {

    private final BluetoothType bluetoothType;
    private DBusConnection dbusConnection;
    private final String dbusPath;

    public AbstractBluetoothObject(BluetoothType _bluetoothType, DBusConnection _dbusConnection, String _dbusPath) {
        bluetoothType = _bluetoothType;
        dbusConnection = _dbusConnection;
        dbusPath = _dbusPath;
    }

    /**
     * DBus-Interface class used in this wrapper object.
     * @return
     */
    protected abstract Class<? extends DBusInterface> getInterfaceClass();

    public BluetoothType getBluetoothType() {
        return bluetoothType;
    }

    public String getDbusPath() {
        return dbusPath;
    }

    protected DBusConnection getDbusConnection() {
        return dbusConnection;
    }

    /**
     * Helper to get remote objects from DBus.
     * @param _objectNames Set of object names to retrieve [e.g service0000, service0001]
     * @param _parentPath DBus parent path (objectName will be appended) [e.g. /org/bluez/hci0]
     * @param _type Expected DBusInterface type [e.g. Device1]
     * @return
     */
    protected <T extends DBusInterface> Map<String, T> getRemoteObjects(Set<String> _objectNames, String _parentPath, Class<T> _type) {
        Map<String, T> map = new LinkedHashMap<>();
        String path = _parentPath;
        // append slash to parent path if missing
        if (!_parentPath.endsWith("/")) {
            path += "/";
        }
        // iterate all object names
        for (String string : _objectNames) {
            T remoteObject = DbusHelper.getRemoteObject(getDbusConnection(), path + string, _type);
            map.put(path + string, remoteObject);
        }
        return map;
    }

    /**
     * Helper to get a value of a DBus property.
     * @param _field DBus property key
     * @param _type expected return type of DBus property
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T getTyped(String _field, Class<T> _type) {
        try {
            Properties remoteObject = dbusConnection.getRemoteObject("org.bluez", dbusPath, Properties.class);
            Object obj = remoteObject.Get(getInterfaceClass().getName(), _field);
            if (ClassUtils.isAssignable(_type, obj.getClass())) {
                return (T) obj;
            }

        } catch (DBusException _ex) {
        }
        return null;
    }

    /**
     * Helper to set a value on a DBus property.
     *
     * @param _field DBus property key
     * @param _value value to set
     */
    protected void setTyped(String _field, Object _value) {
        try {
            Properties remoteObject = dbusConnection.getRemoteObject("org.bluez", dbusPath, Properties.class);
            remoteObject.Set(Adapter1.class.getName(), _field, _value);
        } catch (DBusException _ex) {
        }
    }

    /**
     * Convert options for read/write commands to the correct Map-type.<br>
     * DBus library uses a custom object class names 'variant' which is some sort of wrapper<br>
     * for any object. As the variant class 'allows' specifying the underlying object type, it produces<br>
     * lots of compiler warnings (rawtype).<br>
     * <br>
     * To get around that ugly behavior, this library uses Maps with object value.<br>
     * This method will convert the object values to variant values.
     * @param _options
     * @return
     */
    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    protected Map<String, Variant> optionsToVariantMap(Map<String, Object> _options) {
        Map<String, Variant> optionMap = new LinkedHashMap<>();
        if (_options != null) {
            for (Entry<String, Object> entry : _options.entrySet()) {
                if (entry.getValue() == null) { // null values cannot be wrapped
                    continue;
                }
                optionMap.put(entry.getKey(), new Variant(entry.getValue()));
            }
        }
        return optionMap;
    }
}
