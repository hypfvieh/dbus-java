package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezAlreadyConnectedException;
import org.bluez.exceptions.BluezConnectionAttemptFailedException;
import org.bluez.exceptions.BluezFailedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: network-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.Network1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX
 * 
 * Supported properties: 
 * 
 * 		boolean Connected [readonly]
 * 
 * 			Indicates if the device is connected.
 * 
 * 		string Interface [readonly]
 * 
 * 			Indicates the network interface name when available.
 * 
 * 		string UUID [readonly]
 * 
 * 			Indicates the connection role when available.
 * 
 * 
 * 
 */
public interface Network1 extends DBusInterface {

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
     * @throws BluezAlreadyConnectedException
     * @throws BluezConnectionAttemptFailedException
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
     * @throws BluezFailedException
     */
    void Disconnect() throws BluezFailedException;

}
