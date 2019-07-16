package com.github.hypfvieh.bluetooth.wrapper;

import org.bluez.Battery1;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Wrapper class which represents the battery of a remote bluetooth device.
 *
 */
public class BluetoothBattery extends AbstractBluetoothObject {

	private final Battery1 rawbattery;
	private final BluetoothDevice device;

	public BluetoothBattery(Battery1 _battery, BluetoothDevice _device, String _dbusPath, DBusConnection _dbusConnection) {
		super(BluetoothDeviceType.BATTERY, _dbusConnection, _dbusPath);
		rawbattery = _battery;
		device = _device;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Class<? extends DBusInterface> getInterfaceClass() {
		return Battery1.class;
	}

	/**
	 * Get {@link BluetoothDevice} object where this {@link BluetoothBattery} object belongs to.
	 * @return device
	 */
	public BluetoothDevice getDevice() {
		return device;
	}

	/**
	 * Get the raw {@link Battery1} object wrapped by this {@link BluetoothBattery} object.
	 * @return rawbattery
	 */
	public Battery1 getRawBattery() {
		return rawbattery;
	}

	/**
	 * <b>From bluez Documentation:</b>
	 * <p>
	 * The percentage of battery left as an unsigned 8-bit integer.
	 * </p>
	 * @return byte, maybe null
	 */
	public Byte getPercentage() {
		return getTyped("Percentage", Byte.class);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [battery=" + rawbattery + ", device=" + device.getDbusPath() + ", getBluetoothType()=" + getBluetoothType().name() + ", getDbusPath()=" + getDbusPath() + "]";
	}
}
