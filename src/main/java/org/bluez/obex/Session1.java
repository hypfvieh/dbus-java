package org.bluez.obex;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez.obex<br>
 * <b>Interface:</b> org.bluez.obex.Session1<br>
 * <br>
 * <b>Object path:</b><br>
 *             /org/bluez/obex/server/session{0, 1, 2, ...} or<br>
 *             /org/bluez/obex/client/session{0, 1, 2, ...}<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string Source [readonly]<br>
 * <br>
 * 			Bluetooth adapter address<br>
 * <br>
 * 		string Destination [readonly]<br>
 * <br>
 * 			Bluetooth device address<br>
 * <br>
 * 		byte Channel [readonly]<br>
 * <br>
 * 			Bluetooth channel<br>
 * <br>
 * 		string Target [readonly]<br>
 * <br>
 * 			Target UUID<br>
 * <br>
 * 		string Root [readonly]<br>
 * <br>
 * 			Root path<br>
 * <br>
 * <br>
 * <br>
 */
public interface Session1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Get remote device capabilities.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    String GetCapabilities() throws BluezNotSupportedException, BluezFailedException;

}
