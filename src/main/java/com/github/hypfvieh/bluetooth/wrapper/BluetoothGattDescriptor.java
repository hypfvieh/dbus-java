package com.github.hypfvieh.bluetooth.wrapper;

import java.util.Map;
import java.util.Vector;

import org.bluez.GattDescriptor1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;

/**
 * Wrapper class which represents a GATT descriptor on a remote device.
 *
 * @author hypfvieh
 *
 */
public class BluetoothGattDescriptor extends AbstractBluetoothObject {

    private final GattDescriptor1 descriptor;
    private final BluetoothGattCharacteristic characteristicWrapper;

    public BluetoothGattDescriptor(GattDescriptor1 _descriptor, BluetoothGattCharacteristic _characteristicWrapper, String _dbusPath, DBusConnection _dbusConnection) {
        super(BluetoothDeviceType.GATT_DESCRIPTOR, _dbusConnection, _dbusPath);
        characteristicWrapper = _characteristicWrapper;
        descriptor = _descriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends DBusInterface> getInterfaceClass() {
        return GattDescriptor1.class;
    }

    /**
     * Write value to the GATT descriptor register.<br>
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
        descriptor.WriteValue(_value, optionsToVariantMap(_options));
    }

    /**
     * Read a value from the GATT descriptor register.<br>
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
        return descriptor.ReadValue(optionsToVariantMap(_options));
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * 128-bit descriptor UUID.
     * </p>
     */
    public String getUuid() {
        return getTyped("UUID", String.class);
    }

    /**
     * Get the {@link BluetoothGattCharacteristic} instance behind this {@link BluetoothGattDescriptor} object.
     * @return
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristicWrapper;
    }

    /**
     * Get the raw {@link GattDescriptor1} object behind this wrapper.
     * @return
     */
    public GattDescriptor1 getRawCharacteric() {
        return descriptor;
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * The cached value of the descriptor. This property<br>
     * gets updated only after a successful read request, upon<br>
     * which a PropertiesChanged signal will be emitted.
     * </p>
     */
    public byte[] getValue() {
        Vector<?> typed = getTyped("UUIDs", Vector.class);
        if (typed != null) {
            return byteVectorToByteArray(typed);
        }
        return null;
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Defines how the descriptor value can be used.<br>
     * <i>Possible values:</i>
     * <pre>
     *      "read"
     *      "write"
     *      "encrypt-read"
     *      "encrypt-write"
     *      "encrypt-authenticated-read"
     *      "encrypt-authenticated-write"
     *      "secure-read" (Server Only)
     *      "secure-write" (Server Only)
     * </pre>
     * </p>
     */
    public String getFlags() {
        return getTyped("Flags", String.class);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [descriptor=" + descriptor + ", characteristicWrapper="
                    + characteristicWrapper.getDbusPath() + ", getBluetoothType()="
                    + getBluetoothType().name() + ", getDbusPath()=" + getDbusPath() + "]";
    }


}
