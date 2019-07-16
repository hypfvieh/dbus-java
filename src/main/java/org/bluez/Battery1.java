package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2019-07-16.<br>
 * Based on bluez Documentation: https://git.kernel.org/pub/scm/bluetooth/bluez.git/tree/doc/battery-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Battery1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		byte Percentage [readonly]<br>
 * <br>
 * 			The percentage of battery left as an unsigned 8-bit integer.<br>
 * <br>
 */
public interface Battery1 extends DBusInterface, Properties {
}
