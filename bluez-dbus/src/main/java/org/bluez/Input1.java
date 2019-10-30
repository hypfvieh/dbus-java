package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: input-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Input1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string ReconnectMode [readonly]<br>
 * <br>
 * 			Determines the Connectability mode of the HID device as<br>
 * 			defined by the HID Profile specification, Section 5.4.2.<br>
 * <br>
 * 			This mode is based in the two properties<br>
 * 			HIDReconnectInitiate (see Section 5.3.4.6) and<br>
 * 			HIDNormallyConnectable (see Section 5.3.4.14) which<br>
 * 			define the following four possible values:<br>
 * <br>
 * 			"none"		Device and host are not required to<br>
 * 					automatically restore the connection.<br>
 * <br>
 * 			"host"		Bluetooth HID host restores connection.<br>
 * <br>
 * 			"device"	Bluetooth HID device restores<br>
 * 					connection.<br>
 * <br>
 * 			"any"		Bluetooth HID device shall attempt to<br>
 * 					restore the lost connection, but<br>
 * 					Bluetooth HID Host may also restore the<br>
 * 					connection.<br>
 * <br>
 */
public interface Input1 extends DBusInterface, Properties {

}
