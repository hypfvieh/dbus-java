package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: gatt-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.GattService1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX
 * 
 * Supported properties: 
 * 
 * 		string UUID [read-only]
 * 
 * 			128-bit service UUID.
 * 
 * 		boolean Primary [read-only]
 * 
 * 			Indicates whether or not this GATT service is a
 * 			primary service. If false, the service is secondary.
 * 
 * 		object Device [read-only, optional]
 * 
 * 			Object path of the Bluetooth device the service
 * 			belongs to. Only present on services from remote
 * 			devices.
 * 
 * 		array{object} Includes [read-only]: Not implemented
 * 
 * 			Array of object paths representing the included
 * 			services of this service.
 * 
 * 
 * 
 */
public interface GattService1 extends DBusInterface {

}
