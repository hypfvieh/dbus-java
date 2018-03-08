package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezFailedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: sap-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.SimAccess1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}
 * 
 * Supported properties: 
 * 
 * 		boolean Connected [readonly]
 * 
 * 			Indicates if SAP client is connected to the server.
 * 
 */
public interface SimAccess1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Disconnects SAP client from the server.<br>
     * <br>
     * 
     * @throws BluezFailedException
     */
    void Disconnect() throws BluezFailedException;

}
