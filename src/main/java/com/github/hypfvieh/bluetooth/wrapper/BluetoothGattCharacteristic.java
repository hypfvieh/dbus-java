package com.github.hypfvieh.bluetooth.wrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.bluez.GattCharacteristic1;
import org.bluez.GattDescriptor1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusInterface;

import com.github.hypfvieh.DbusHelper;

/**
 * Wrapper class which represents a GATT characteristic on a remote device.
 *
 * @author hypfvieh
 *
 */
public class BluetoothGattCharacteristic extends AbstractBluetoothObject {

    private final GattCharacteristic1 gattCharacteristic;
    private final BluetoothGattService gattService;

    private final Map<String, BluetoothGattDescriptor> descriptorByUuid = new LinkedHashMap<>();

    public BluetoothGattCharacteristic(GattCharacteristic1 _gattCharacteristic, BluetoothGattService _service, String _dbusPath, DBusConnection _dbusConnection) {
        super(BluetoothDeviceType.GATT_CHARACTERISTIC, _dbusConnection, _dbusPath);

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
     * Will issue a query if {@link #refreshGattCharacteristics()} wasn't called before.
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
     * @param _uuid uuid
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
     * @param _value value to write
     * @param _options options to use
     * @throws BluezFailedException if operation failed
     * @throws BluezInProgressException if operation is already in progress
     * @throws BluezNotPermittedException if operation is not permitted
     * @throws BluezNotAuthorizedException if not authorized
     * @throws BluezNotSupportedException if not supported
     * @throws BluezInvalidValueLengthException
     */
    public void writeValue(byte[] _value, Map<String, Object> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException, BluezInvalidValueLengthException {
        gattCharacteristic.WriteValue(_value, optionsToVariantMap(_options));
    }

    /**
     * Read a value from the GATT characteristics register.<br>
     * Supported options:<br>
     * <pre>
     * "offset": uint16 offset
     * "device": Object Device (Server only)
     * </pre>
     * @param _options options to use
     * @return byte array, maybe null
     * @throws BluezFailedException if anything failed
     * @throws BluezInProgressException if already in progress
     * @throws BluezNotPermittedException if not permitted
     * @throws BluezNotAuthorizedException if not authorized
     * @throws BluezNotSupportedException if not supported
     */
    public byte[] readValue(Map<String, Object> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException  {
        return gattCharacteristic.ReadValue(optionsToVariantMap(_options));
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * 128-bit characteristic UUID.
     * </p>
     * @return uuid, maybe null
     */
    public String getUuid() {
        return getTyped("UUID", String.class);
    }

    /**
     * Returns the {@link BluetoothGattService} object which provides this {@link BluetoothGattCharacteristic}.
     * @return GattService, maybe null
     */
    public BluetoothGattService getService() {
        return gattService;
    }

    /**
     * Get the raw {@link GattCharacteristic1} object behind this wrapper.
     * @return {@link GattCharacteristic1}, maybe null
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
     * @return cached characteristics value, maybe null
     */
    public byte[] getValue() {
        Vector<?> typed = getTyped("UUIDs", Vector.class);
        if (typed != null) {
            return byteVectorToByteArray(typed);
        }
        return null;
    }

    /**
     * From bluez Documentation:<br>
     * True, if notifications or indications on this characteristic are currently enabled.
     * @return maybe null if feature is not supported
     */
    public Boolean isNotifying() {
        return getTyped("Notifying", Boolean.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Defines how the characteristic value can be used. See<br>
     * Core spec "Table 3.5: Characteristic Properties bit<br>
     * field", and "Table 3.8: Characteristic Extended<br>
     * Properties bit field".
     * <br>
     * </p>
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
     * @return string, maybe null
     */
    public String getFlags() {
        return getTyped("Flags", String.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Starts a notification session from this characteristic
     * if it supports value notifications or indications.
     * <br>
     * </p>
     * @throws BluezFailedException if operation failed
     * @throws BluezInProgressException if operation already in progress
     * @throws BluezNotSupportedException if operation is not supported
     * @throws BluezNotPermittedException
     */
    public void startNotify() throws BluezFailedException, BluezInProgressException, BluezNotSupportedException, BluezNotPermittedException {
        gattCharacteristic.StartNotify();
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * This method will cancel any previous StartNotify
     * transaction. Note that notifications from a
     * characteristic are shared between sessions thus
     * calling StopNotify will release a single session.
     * <br>
     * </p>
     * @throws BluezFailedException on any error
     */
    public void stopNotify() throws BluezFailedException {
        gattCharacteristic.StopNotify();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [gattCharacteristic=" + gattCharacteristic
                + ", gattService=" + gattService.getDbusPath() + ", getBluetoothType()="
                + getBluetoothType().name() + ", getDbusPath()=" + getDbusPath() + "]";
    }


}
