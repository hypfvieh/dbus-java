package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezInvalidLengthException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: advertising-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.LEAdvertisingManager1<br>
 * <br>
 * <b>Object path:</b><br>
 *             /org/bluez/{hci0,hci1,...}<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		byte ActiveInstances<br>
 * <br>
 * 			Number of active advertising instances.<br>
 * <br>
 * 		byte SupportedInstances<br>
 * <br>
 * 			Number of available advertising instances.<br>
 * <br>
 * 		array{string} SupportedIncludes<br>
 * <br>
 * 			List of supported system includes.<br>
 * <br>
 * 			Possible values: "tx-power"<br>
 * 					 "appearance"<br>
 * 					 "local-name"<br>
 * <br>
 */
public interface LEAdvertisingManager1 extends DBusInterface, Properties {

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
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezAlreadyExistsException when item already exists
     * @throws BluezInvalidLengthException
     * @throws BluezNotPermittedException
     */
    void RegisterAdvertisement(DBusPath _advertisement, Map<String, Variant<?>> _options) throws BluezInvalidArgumentsException, BluezAlreadyExistsException, BluezInvalidLengthException, BluezNotPermittedException;

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
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezDoesNotExistException when item does not exist
     */
    void UnregisterAdvertisement(DBusPath _advertisement) throws BluezInvalidArgumentsException, BluezDoesNotExistException;

}
