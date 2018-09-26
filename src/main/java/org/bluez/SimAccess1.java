package org.bluez;

import org.bluez.exceptions.BluezFailedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: sap-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.SimAccess1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		boolean Connected [readonly]<br>
 * <br>
 * 			Indicates if SAP client is connected to the server.<br>
 * <br>
 */
public interface SimAccess1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Disconnects SAP client from the server.<br>
     * <br>
     * 
     * @throws BluezFailedException on failure
     */
    void Disconnect() throws BluezFailedException;

}
