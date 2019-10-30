package org.bluez;

import org.bluez.exceptions.BluezAlreadyConnectedException;
import org.bluez.exceptions.BluezConnectionAttemptFailedException;
import org.bluez.exceptions.BluezFailedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: network-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Network1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		boolean Connected [readonly]<br>
 * <br>
 * 			Indicates if the device is connected.<br>
 * <br>
 * 		string Interface [readonly]<br>
 * <br>
 * 			Indicates the network interface name when available.<br>
 * <br>
 * 		string UUID [readonly]<br>
 * <br>
 * 			Indicates the connection role when available.<br>
 * <br>
 * <br>
 * <br>
 */
public interface Network1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Connect to the network device and return the network<br>
     * interface name. Examples of the interface name are<br>
     * bnep0, bnep1 etc.<br>
     * <br>
     * uuid can be either one of "gn", "panu" or "nap" (case<br>
     * insensitive) or a traditional string representation of<br>
     * UUID or a hexadecimal number.<br>
     * <br>
     * The connection will be closed and network device<br>
     * released either upon calling Disconnect() or when<br>
     * the client disappears from the message bus.<br>
     * <br>
     * 
     * @param _uuid
     * 
     * @throws BluezAlreadyConnectedException when already connected
     * @throws BluezConnectionAttemptFailedException when connection attempt failed
     */
    String Connect(String _uuid) throws BluezAlreadyConnectedException, BluezConnectionAttemptFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Disconnect from the network device.<br>
     * <br>
     * To abort a connection attempt in case of errors or<br>
     * timeouts in the client it is fine to call this method.<br>
     * <br>
     * 
     * @throws BluezFailedException on failure
     */
    void Disconnect() throws BluezFailedException;

}
