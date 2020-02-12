package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2020-02-12.<br>
 * Based on bluez Documentation: thermometer-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Thermometer1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		boolean Intermediate [readonly]<br>
 * <br>
 * 			True if the thermometer supports intermediate<br>
 * 			measurement notifications.<br>
 * <br>
 * 		uint16 Interval (optional) [readwrite]<br>
 * <br>
 * 			The Measurement Interval defines the time (in<br>
 * 			seconds) between measurements. This interval is<br>
 * 			not related to the intermediate measurements and<br>
 * 			must be defined into a valid range. Setting it<br>
 * 			to zero means that no periodic measurements will<br>
 * 			be taken.<br>
 * <br>
 * 		uint16 Maximum (optional) [readonly]<br>
 * <br>
 * 			Defines the maximum value allowed for the interval<br>
 * 			between periodic measurements.<br>
 * <br>
 * 		uint16 Minimum (optional) [readonly]<br>
 * <br>
 * 			Defines the minimum value allowed for the interval<br>
 * 			between periodic measurements.<br>
 * <br>
 * <br>
 * <br>
 */
public interface Thermometer1 extends DBusInterface {

}
