package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;
import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezInvalidLengthException;
import org.bluez.exceptions.BluezNotPermittedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: advertising-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.LEAdvertisingManager1
 * 
 * Object path: 
 *             /org/bluez/{hci0,hci1,...}
 * 
 * Supported properties: 
 * 
 * 		byte ActiveInstances
 * 
 * 			Number of active advertising instances.
 * 
 * 		byte SupportedInstances
 * 
 * 			Number of available advertising instances.
 * 
 * 		array{string} SupportedIncludes
 * 
 * 			List of supported system includes.
 * 
 * 			Possible values: "tx-power"
 * 					 "appearance"
 * 					 "local-name"
 * 
 */
public interface LEAdvertisingManager1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Registers an advertisement object to be sent over the LE<br>
     * Advertising channel.  The service must be exported<br>
     * under interface LEAdvertisement1.<br>
     * <br>
     * InvalidArguments error indicates that the object has<br>
     * invalid or conflicting properties.<br>
     * <br>
     * InvalidLength error indicates that the data<br>
     * provided generates a data packet which is too long.<br>
     * <br>
     * The properties of this object are parsed when it is<br>
     * registered, and any changes are ignored.<br>
     * <br>
     * If the same object is registered twice it will result in<br>
     * an AlreadyExists error.<br>
     * <br>
     * If the maximum number of advertisement instances is<br>
     * reached it will result in NotPermitted error.<br>
     * <br>
     * 
     * @param _advertisement
     * @param _options
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezAlreadyExistsException
     * @throws BluezInvalidLengthException
     * @throws BluezNotPermittedException
     */
    void RegisterAdvertisement(Object _advertisement, Map<?, ?> _options) throws BluezInvalidArgumentsException, BluezAlreadyExistsException, BluezInvalidLengthException, BluezNotPermittedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This unregisters an advertisement that has been<br>
     * previously registered.  The object path parameter must<br>
     * match the same value that has been used on registration.<br>
     * <br>
     * 
     * @param _advertisement
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezDoesNotExistException
     */
    void UnregisterAdvertisement(Object _advertisement) throws BluezInvalidArgumentsException, BluezDoesNotExistException;

}
