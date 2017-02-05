package org.caseof.bluetooth.wrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bluez.GattCharacteristic1;
import org.bluez.GattService1;
import org.bluez.exceptions.BluezNotImplementedException;
import org.caseof.DbusHelper;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;

/**
 * Wrapper class which represents a GATT service provided by a remote device.
 * @author maniac
 *
 */
public class BluetoothGattService extends AbstractBluetoothObject {

    private final GattService1 service;
    private final BluetoothDevice device;

    private final Map<String, BluetoothGattCharacteristic> characteristicByUuid = new LinkedHashMap<>();

    public BluetoothGattService(GattService1 _service, BluetoothDevice _device, String _dbusPath, DBusConnection _dbusConnection) {
        super(BluetoothType.GATT_SERVICE, _dbusConnection, _dbusPath);
        service = _service;
        device = _device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends DBusInterface> getInterfaceClass() {
        return GattService1.class;
    }

    /**
     * Re-queries the GattCharacteristics from the device.
     */
    public void refreshGattCharacteristics() {
        characteristicByUuid.clear();

        Set<String> findNodes = DbusHelper.findNodes(getDbusConnection(), getDbusPath());
        Map<String, GattCharacteristic1> remoteObjects = getRemoteObjects(findNodes, getDbusPath(), GattCharacteristic1.class);
        for (Entry<String, GattCharacteristic1> entry : remoteObjects.entrySet()) {
            BluetoothGattCharacteristic bluetoothGattCharacteristics = new BluetoothGattCharacteristic(entry.getValue(), this, entry.getKey(), getDbusConnection());
            characteristicByUuid.put(bluetoothGattCharacteristics.getUuid(), bluetoothGattCharacteristics);
        }
    }

    /**
     * Get the currently available GATT characteristics.<br>
     * Will issue a query if {@link #refreshGattCharacteristics()} wasn't called before.
     * @return List, maybe empty but never null
     */
    public List<BluetoothGattCharacteristic> getGattCharacteristics() {
        if (characteristicByUuid.isEmpty()) {
            refreshGattCharacteristics();
        }
        return new ArrayList<>(characteristicByUuid.values());
    }

    /**
     * Return the {@link BluetoothGattCharacteristic} object for the given UUID.
     * @param _uuid
     * @return maybe null if not found
     */
    public BluetoothGattCharacteristic getGattCharacteristicByUuid(String _uuid) {
        if (characteristicByUuid.isEmpty()) {
            refreshGattCharacteristics();
        }
        return characteristicByUuid.get(_uuid);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * 128-bit service UUID.
     * </p>
     * @return
     */
    public String getUuid() {
        return getTyped("UUID", String.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Indicates whether or not this GATT service is a<br>
     * primary service. If false, the service is secondary.
     * </p>
     * @return
     */
    public boolean isPrimary() {
        return getTyped("Primary", boolean.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * <b>Not implemented</b><br>
     * Array of object paths representing the included<br>
     * services of this service.
     * </p>
     * @return
     */
    public Object[] getIncludes() throws BluezNotImplementedException {
        throw new BluezNotImplementedException("Feature not yet implemented");
    }

    /**
     * Get the raw {@link GattService1} object.
     * @return
     */
    public GattService1 getService() {
        return service;
    }

    /**
     * Get the {@link BluetoothDevice} instance which is providing this {@link BluetoothGattService}.
     * @return
     */
    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [service=" + service + ", device=" + device.getDbusPath()
            + ", getBluetoothType()=" + getBluetoothType().name() + ", getDbusPath()=" + getDbusPath() + "]";
    }



}
