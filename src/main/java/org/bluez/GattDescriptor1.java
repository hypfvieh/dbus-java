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
 * File generated - 2018-03-08.
 * Based on bluez Documentation: gatt-api.txt.
 *
 * Service: org.bluez
 * Interface: org.bluez.GattDescriptor1
 *
 * Object path:
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX/charYYYY/descriptorZZZ
 *
 * Supported properties:
 *
 * 		string UUID [read-only]
 *
 * 			128-bit descriptor UUID.
 *
 * 		object Characteristic [read-only]
 *
 * 			Object path of the GATT characteristic the descriptor
 * 			belongs to.
 *
 * 		array{byte} Value [read-only, optional]
 *
 * 			The cached value of the descriptor. This property
 * 			gets updated only after a successful read request, upon
 * 			which a PropertiesChanged signal will be emitted.
 *
 * 		array{string} Flags [read-only]
 *
 * 			Defines how the descriptor value can be used.
 *
 * 			Possible values:
 *
 * 				"read"
 * 				"write"
 * 				"encrypt-read"
 * 				"encrypt-write"
 * 				"encrypt-authenticated-read"
 * 				"encrypt-authenticated-write"
 * 				"secure-read" (Server Only)
 * 				"secure-write" (Server Only)
 *
 *
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
