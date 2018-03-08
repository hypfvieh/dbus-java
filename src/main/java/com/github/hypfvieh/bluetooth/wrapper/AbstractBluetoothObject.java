package com.github.hypfvieh.bluetooth.wrapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang3.ClassUtils;
import org.bluez.Adapter1;
import org.freedesktop.dbus.SignalAwareProperties;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.DbusHelper;

/**
 * Base class of all bluetooth wrapper object classes.
 *
 * @author hypfvieh
 *
 */
public abstract class AbstractBluetoothObject {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BluetoothDeviceType bluetoothType;
    private DBusConnection dbusConnection;
    private final String dbusPath;

    public AbstractBluetoothObject(BluetoothDeviceType _bluetoothType, DBusConnection _dbusConnection, String _dbusPath) {
        bluetoothType = _bluetoothType;
        dbusConnection = _dbusConnection;
        dbusPath = _dbusPath;
    }

    /**
     * DBus-Interface class used in this wrapper object.
     * @return class which implements the interface
     */
    protected abstract Class<? extends DBusInterface> getInterfaceClass();

    public BluetoothDeviceType getBluetoothType() {
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
     * @param <T> class/interface extending {@link DBusInterface}
     * @return Map of string key and the given interface
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
     * @param <T> class of the expected result
     * @return value of _field as _type class or null
     */
    protected <T> T getTyped(String _field, Class<T> _type) {
        try {
            SignalAwareProperties remoteObject = dbusConnection.getRemoteObject("org.bluez", dbusPath, SignalAwareProperties.class);
            Object obj = remoteObject.Get(getInterfaceClass().getName(), _field);
            if (ClassUtils.isAssignable(_type, obj.getClass())) {
                return _type.cast(obj);
            }

        } catch (DBusException _ex) {
            logger.trace("Error while receiving data from DBUS (Field: {}, Type: {}).", _field, _type, _ex);
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
            SignalAwareProperties remoteObject = dbusConnection.getRemoteObject("org.bluez", dbusPath, SignalAwareProperties.class);
            remoteObject.Set(Adapter1.class.getName(), _field, _value);
        } catch (DBusException _ex) {
            logger.trace("Error while setting data for DBUS (Field: {}, Value: {}).", _field, _value, _ex);
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
     * @param _options options map
     * @return map of variant, maybe empty but never null
     */
    protected Map<String, Variant<?>> optionsToVariantMap(Map<String, Object> _options) {
        Map<String, Variant<?>> optionMap = new LinkedHashMap<>();
        if (_options != null) {
            for (Entry<String, Object> entry : _options.entrySet()) {
                if (entry.getValue() == null) { // null values cannot be wrapped
                    continue;
                }
                optionMap.put(entry.getKey(), new Variant<>(entry.getValue()));
            }
        }
        return optionMap;
    }


    protected byte[] byteVectorToByteArray(Vector<?> _vector) {
        if (_vector == null) {
            return null;
        }
        if (_vector.isEmpty()) {
            return new byte[] {};
        }

        if (!ClassUtils.isAssignable(byte.class, _vector.get(0).getClass())) {
            return null;
        }

        byte[] result = new byte[_vector.size()];
        for (int i = 0; i < _vector.size(); i++) {
            Object x = _vector.get(i);
            result[i] = (byte) x;
        }

        return result;
    }

    /**
     * Convert Byte[] to byte[] array.
     * @param oBytes the array to convert
     * @return primitive byte array
     */
    protected byte[] toPrimitives(Byte[] oBytes) {
        byte[] bytes = new byte[oBytes.length];

        for (int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }
}
