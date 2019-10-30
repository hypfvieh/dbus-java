package com.github.hypfvieh.bluetooth.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bluez.GattDescriptor1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusInterface;

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
     * @param _value value to write
     * @param _options options to use
     * @throws BluezFailedException on failure if anything failed
     * @throws BluezInProgressException when operation already in progress if operation in progress
     * @throws BluezNotPermittedException if operation not permitted
     * @throws BluezNotAuthorizedException when not authorized if not authorized
     * @throws BluezNotSupportedException when operation not supported if not supported
     * @throws BluezInvalidValueLengthException
     */
    public void writeValue(byte[] _value, Map<String, Object> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException, BluezInvalidValueLengthException {
        descriptor.WriteValue(_value, optionsToVariantMap(_options));
    }

    /**
     * Read a value from the GATT descriptor register.<br>
     * Supported options:<br>
     * <pre>
     * "offset": uint16 offset
     * "device": Object Device (Server only)
     * </pre>
     * @param _options options to use
     * @return byte array, maybe null
     * @throws BluezFailedException on failure if anything failed
     * @throws BluezInProgressException when operation already in progress if operation in progress
     * @throws BluezNotPermittedException if operation not permitted
     * @throws BluezNotAuthorizedException when not authorized if not authorized
     * @throws BluezNotSupportedException when operation not supported if not supported
     */
    public byte[] readValue(Map<String, Object> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException  {
        return descriptor.ReadValue(optionsToVariantMap(_options));
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * 128-bit descriptor UUID.
     * </p>
     * @return uuid, maybe null
     */
    public String getUuid() {
        return getTyped("UUID", String.class);
    }

    /**
     * Get the {@link BluetoothGattCharacteristic} instance behind this {@link BluetoothGattDescriptor} object.
     * @return {@link BluetoothGattCharacteristic}, maybe null
     */
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristicWrapper;
    }

    /**
     * Get the raw {@link GattDescriptor1} object behind this wrapper.
     * @return {@link GattDescriptor1}, maybe null
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
     * @return byte array, maybe null
     */
    public byte[] getValue() {
        List<?> typed = getTyped("UUIDs", ArrayList.class);
        if (typed != null) {
            return byteListToByteArray(typed);
        }
        return null;
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Defines how the descriptor value can be used.<br>
     * </p>
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
     * @return string, maybe null
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
