package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.io.FileDescriptor;
import org.bluez.exceptions.BluezNotAcquiredException;
import org.bluez.exceptions.BluezNotAllowedException;
import org.bluez.exceptions.BluezNotConnectedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: health-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.HealthChannel1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/chanZZZ
 * 
 * Supported properties: 
 * 
 * 		string Type [readonly]
 * 
 * 			The quality of service of the data channel. ("reliable"
 * 			or "streaming")
 * 
 * 		object Device [readonly]
 * 
 * 			Identifies the Remote Device that is connected with.
 * 			Maps with a HealthDevice object.
 * 
 * 		object Application [readonly]
 * 
 * 			Identifies the HealthApplication to which this channel
 * 			is related to (which indirectly defines its role and
 * 			data type).
 * 
 */
public interface HealthChannel1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Returns the file descriptor for this data channel. If<br>
     * the data channel is not connected it will also<br>
     * reconnect.<br>
     * <br>
     * 
     * @throws BluezNotConnectedException
     * @throws BluezNotAllowedException
     */
    FileDescriptor Acquire() throws BluezNotConnectedException, BluezNotAllowedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Releases the fd. Application should also need to<br>
     * close() it.<br>
     * <br>
     * 
     * @throws BluezNotAcquiredException
     * @throws BluezNotAllowedException
     */
    void Release() throws BluezNotAcquiredException, BluezNotAllowedException;

}
