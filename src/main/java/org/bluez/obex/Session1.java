package org.bluez.obex;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotSupportedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: obex-api.txt.
 * 
 * Service: org.bluez.obex
 * Interface: org.bluez.obex.Session1
 * 
 * Object path: 
 *             /org/bluez/obex/server/session{0, 1, 2, ...} or
 *             /org/bluez/obex/client/session{0, 1, 2, ...}
 * 
 * Supported properties: 
 * 
 * 		string Source [readonly]
 * 
 * 			Bluetooth adapter address
 * 
 * 		string Destination [readonly]
 * 
 * 			Bluetooth device address
 * 
 * 		byte Channel [readonly]
 * 
 * 			Bluetooth channel
 * 
 * 		string Target [readonly]
 * 
 * 			Target UUID
 * 
 * 		string Root [readonly]
 * 
 * 			Root path
 * 
 * 
 * 
 */
public interface Session1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Get remote device capabilities.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    String GetCapabilities() throws BluezNotSupportedException, BluezFailedException;

}
