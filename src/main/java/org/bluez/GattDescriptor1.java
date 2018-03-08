package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-03-08.<br>
 * Based on bluez Documentation: gatt-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.GattDescriptor1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX/charYYYY/descriptorZZZ<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string UUID [read-only]<br>
 * <br>
 * 			128-bit descriptor UUID.<br>
 * <br>
 * 		object Characteristic [read-only]<br>
 * <br>
 * 			Object path of the GATT characteristic the descriptor<br>
 * 			belongs to.<br>
 * <br>
 * 		array{byte} Value [read-only, optional]<br>
 * <br>
 * 			The cached value of the descriptor. This property<br>
 * 			gets updated only after a successful read request, upon<br>
 * 			which a PropertiesChanged signal will be emitted.<br>
 * <br>
 * 		array{string} Flags [read-only]<br>
 * <br>
 * 			Defines how the descriptor value can be used.<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				"read"<br>
 * 				"write"<br>
 * 				"encrypt-read"<br>
 * 				"encrypt-write"<br>
 * 				"encrypt-authenticated-read"<br>
 * 				"encrypt-authenticated-write"<br>
 * 				"secure-read" (Server Only)<br>
 * 				"secure-write" (Server Only)<br>
 * <br>
 * <br>
 */
public interface GattDescriptor1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Issues a request to read the value of the<br>
     * characteristic and returns the value if the<br>
     * operation was successful.<br>
     * <br>
     * Possible options: "offset": Start offset<br>
     * 		  "device": Device path (Server only)<br>
     * 		  "link": Link type (Server only)<br>
     * <br>
     *
     * @param _flags
     *
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezNotPermittedException
     * @throws BluezNotAuthorizedException
     * @throws BluezNotSupportedException
     */
    byte[] ReadValue(Map<String, Variant<?>> _flags) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Issues a request to write the value of the<br>
     * characteristic.<br>
     * <br>
     * Possible options: "offset": Start offset<br>
     * 		  "device": Device path (Server only)<br>
     * 		  "link": Link type (Server only)<br>
     * <br>
     *
     * @param _value
     * @param _flags
     *
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezNotPermittedException
     * @throws BluezInvalidValueLengthException
     * @throws BluezNotAuthorizedException
     * @throws BluezNotSupportedException
     */
    void WriteValue(byte[] _value, Map<String, Variant<?>> _flags) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException;
}
