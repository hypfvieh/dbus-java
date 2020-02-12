package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2020-02-12.<br>
 * Based on bluez Documentation: gatt-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.GattService1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string UUID [read-only]<br>
 * <br>
 * 			128-bit service UUID.<br>
 * <br>
 * 		boolean Primary [read-only]<br>
 * <br>
 * 			Indicates whether or not this GATT service is a<br>
 * 			primary service. If false, the service is secondary.<br>
 * <br>
 * 		object Device [read-only, optional]<br>
 * <br>
 * 			Object path of the Bluetooth device the service<br>
 * 			belongs to. Only present on services from remote<br>
 * 			devices.<br>
 * <br>
 * 		array{object} Includes [read-only, optional]<br>
 * <br>
 * 			Array of object paths representing the included<br>
 * 			services of this service.<br>
 * <br>
 * 		uint16 Handle [read-write, optional] (Server Only)<br>
 * <br>
 * 			Service handle. When available in the server it<br>
 * 			would attempt to use to allocate into the database<br>
 * 			which may fail, to auto allocate the value 0x0000<br>
 * 			shall be used which will cause the allocated handle to<br>
 * 			be set once registered.<br>
 * <br>
 * <br>
 * <br>
 */
public interface GattService1 extends DBusInterface {

}
