package org.caseof.bluetooth.wrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bluez.GattCharacteristic1;
import org.bluez.GattDescriptor1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.caseof.DbusHelper;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;

/**
 * Wrapper class which represents a GATT characteristic on a remote device.
 *
 * @author maniac
 *
 */
public class BluetoothGattCharacteristic extends AbstractBluetoothObject {

    private final GattCharacteristic1 gattCharacteristic;
    private final BluetoothGattService gattService;

    private final Map<String, BluetoothGattDescriptor> descriptorByUuid = new LinkedHashMap<>();

    public BluetoothGattCharacteristic(GattCharacteristic1 _gattCharacteristic, BluetoothGattService _service, String _dbusPath, DBusConnection _dbusConnection) {
        super(BluetoothType.GATT_CHARACTERISTIC, _dbusConnection, _dbusPath);

        gattCharacteristic = _gattCharacteristic;
        gattService = _service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends DBusInterface> getInterfaceClass() {
        return GattCharacteristic1.class;
    }

    /**
     * Re-queries the GattCharacteristics from the device.
     */
    public void refreshGattCharacteristics() {
        descriptorByUuid.clear();

        Set<String> findNodes = DbusHelper.findNodes(getDbusConnection(), getDbusPath());
        Map<String, GattDescriptor1> remoteObjects = getRemoteObjects(findNodes, getDbusPath(), GattDescriptor1.class);
        for (Entry<String, GattDescriptor1> entry : remoteObjects.entrySet()) {
            BluetoothGattDescriptor btDescriptor = new BluetoothGattDescriptor(entry.getValue(), this, entry.getKey(), getDbusConnection());
            descriptorByUuid.put(btDescriptor.getUuid(), btDescriptor);
        }
    }

    /**
     * Get the currently available GATT descriptors.<br>
     * Will issue a query if {@link #getGattDescriptorByUuid()} wasn't called before.
     * @return List, maybe empty but never null
     */
    public List<BluetoothGattDescriptor> getGattDescriptors() {
        if (descriptorByUuid.isEmpty()) {
            refreshGattCharacteristics();
        }
        return new ArrayList<>(descriptorByUuid.values());
    }

    /**
     * Return the {@link BluetoothGattDescriptor} object for the given UUID.
     * @param _uuid
     * @return maybe null if not found
     */
    public BluetoothGattDescriptor getGattDescriptorByUuid(String _uuid) {
        if (descriptorByUuid.isEmpty()) {
            refreshGattCharacteristics();
        }
        return descriptorByUuid.get(_uuid);
    }

    /**
     * Write value to the GATT characteristic register.<br>
     * Supported options:<br>
     * <pre>
     * "offset": uint16 offset
     * "device": Object Device (Server only)
     * </pre>
     * @param _value
     * @param _options
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezNotPermittedException
     * @throws BluezNotAuthorizedException
     * @throws BluezNotSupportedException
     */
    public void writeValue(byte[] _value, Map<String, Object> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException {
        gattCharacteristic.WriteValue(_value, optionsToVariantMap(_options));
    }

    /**
     * Read a value from the GATT characteristics register.<br>
     * Supported options:<br>
     * <pre>
     * "offset": uint16 offset
     * "device": Object Device (Server only)
     * </pre>
     * @param _options
     * @return
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezNotPermittedException
     * @throws BluezNotAuthorizedException
     * @throws BluezNotSupportedException
     */
    public byte[] readValue(Map<String, Object> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException  {
        return gattCharacteristic.ReadValue(optionsToVariantMap(_options));
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * 128-bit characteristic UUID.
     * </p>
     * @return
     */
    public String getUuid() {
        return getTyped("UUID", String.class);
    }

    /**
     * Returns the {@link BluetoothGattService} object which provides this {@link BluetoothGattCharacteristic}.
     * @return
     */
    public BluetoothGattService getService() {
        return gattService;
    }

    /**
     * Get the raw {@link GattCharacteristic1} object behind this wrapper.
     * @return
     */
    public GattCharacteristic1 getRawGattCharacteristic() {
        return gattCharacteristic;
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * The cached value of the characteristic. This property<br>
     * gets updated only after a successful read request and<br>
     * when a notification or indication is received, upon<br>
     * which a PropertiesChanged signal will be emitted.
     * </p>
     * @return
     */
    public byte[] getValue() {
        return getTyped("Value", byte[].class);
    }

    /**
     * From bluez Documentation:<br>
     * True, if notifications or indications on this characteristic are currently enabled.
     * @return
     */
    public boolean isNotifying() {
        return getTyped("Notifying", boolean.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Defines how the characteristic value can be used. See<br>
     * Core spec "Table 3.5: Characteristic Properties bit<br>
     * field", and "Table 3.8: Characteristic Extended<br>
     * Properties bit field".
     * <br>
     * <pre>
     * Allowed values:
     *         "broadcast"
     *         "read"
     *         "write-without-response"
     *         "write"
     *         "notify"
     *         "indicate"
     *         "authenticated-signed-writes"
     *         "reliable-write"
     *         "writable-auxiliaries"
     *         "encrypt-read"
     *         "encrypt-write"
     *         "encrypt-authenticated-read"
     *         "encrypt-authenticated-write"
     *         "secure-read" (Server only)
     *         "secure-write" (Server only)
     * </pre>
     * </p>
     * @return
     */
    public String getFlags() {
        return getTyped("Flags", String.class);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [gattCharacteristic=" + gattCharacteristic
                + ", gattService=" + gattService.getDbusPath() + ", getBluetoothType()="
                + getBluetoothType().name() + ", getDbusPath()=" + getDbusPath() + "]";
    }


}
