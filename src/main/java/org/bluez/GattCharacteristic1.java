package org.bluez;

import java.io.FileDescriptor;
import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: gatt-api.txt.
 *
 * Service: org.bluez
 * Interface: org.bluez.GattCharacteristic1
 *
 * Object path:
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX/charYYYY
 *
 * Supported properties:
 *
 * 		string UUID [read-only]
 *
 * 			128-bit characteristic UUID.
 *
 * 		object Service [read-only]
 *
 * 			Object path of the GATT service the characteristic
 * 			belongs to.
 *
 * 		array{byte} Value [read-only, optional]
 *
 * 			The cached value of the characteristic. This property
 * 			gets updated only after a successful read request and
 * 			when a notification or indication is received, upon
 * 			which a PropertiesChanged signal will be emitted.
 *
 * 		boolean WriteAcquired [read-only, optional]
 *
 * 			True, if this characteristic has been acquired by any
 * 			client using AcquireWrite.
 *
 * 			For client properties is ommited in case
 * 			'write-without-response' flag is not set.
 *
 * 			For server the presence of this property indicates
 * 			that AcquireWrite is supported.
 *
 * 		boolean NotifyAcquired [read-only, optional]
 *
 * 			True, if this characteristic has been acquired by any
 * 			client using AcquireNotify.
 *
 * 			For client this properties is ommited in case 'notify'
 * 			flag is not set.
 *
 * 			For server the presence of this property indicates
 * 			that AcquireNotify is supported.
 *
 * 		boolean Notifying [read-only, optional]
 *
 * 			True, if notifications or indications on this
 * 			characteristic are currently enabled.
 *
 * 		array{string} Flags [read-only]
 *
 * 			Defines how the characteristic value can be used. See
 * 			Core spec "Table 3.5: Characteristic Properties bit
 * 			field", and "Table 3.8: Characteristic Extended
 * 			Properties bit field". Allowed values:
 *
 * 				"broadcast"
 * 				"read"
 * 				"write-without-response"
 * 				"write"
 * 				"notify"
 * 				"indicate"
 * 				"authenticated-signed-writes"
 * 				"reliable-write"
 * 				"writable-auxiliaries"
 * 				"encrypt-read"
 * 				"encrypt-write"
 * 				"encrypt-authenticated-read"
 * 				"encrypt-authenticated-write"
 * 				"secure-read" (Server only)
 * 				"secure-write" (Server only)
 *
 *
 */
public interface GattCharacteristic1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Issues a request to read the value of the<br>
     * characteristic and returns the value if the<br>
     * operation was successful.<br>
     * <br>
     * Possible options: "offset": uint16 offset<br>
     * 		  "device": Object Device (Server only)<br>
     * <br>
     *
     * @param _options
     *
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezNotPermittedException
     * @throws BluezNotAuthorizedException
     * @throws BluezNotSupportedException
     */
    byte[] ReadValue(Map<String,Variant<?>> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Issues a request to write the value of the<br>
     * characteristic.<br>
     * <br>
     * Possible options: "offset": Start offset<br>
     * 		  "device": Device path (Server only)<br>
     * <br>
     *
     * @param _value
     * @param _options
     *
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezNotPermittedException
     * @throws BluezInvalidValueLengthException
     * @throws BluezNotAuthorizedException
     * @throws BluezNotSupportedException
     */
    void WriteValue(byte[] _value, Map<String,Variant<?>> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Acquire file descriptor and MTU for writing. Usage of<br>
     * WriteValue will be locked causing it to return<br>
     * NotPermitted error.<br>
     * <br>
     * For server the MTU returned shall be equal or smaller<br>
     * than the negotiated MTU.<br>
     * <br>
     * For client it only works with characteristic that has<br>
     * WriteAcquired property which relies on<br>
     * write-without-response Flag.<br>
     * <br>
     * To release the lock the client shall close the file<br>
     * descriptor, a HUP is generated in case the device<br>
     * is disconnected.<br>
     * <br>
     * Note: the MTU can only be negotiated once and is<br>
     * symmetric therefore this method may be delayed in<br>
     * order to have the exchange MTU completed, because of<br>
     * that the file descriptor is closed during<br>
     * reconnections as the MTU has to be renegotiated.<br>
     * <br>
     * Possible options: "device": Object Device (Server only)<br>
     * 		  "MTU": Exchanged MTU (Server only)<br>
     * <br>
     *
     * @param _options
     *
     * @throws BluezFailedException
     * @throws BluezNotSupportedException
     */
    TwoTuple<FileDescriptor, UInt16> AcquireWrite(Map<?, ?> _options) throws BluezFailedException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Acquire file descriptor and MTU for notify. Usage of<br>
     * StartNotify will be locked causing it to return<br>
     * NotPermitted error.<br>
     * <br>
     * For server the MTU returned shall be equal or smaller<br>
     * than the negotiated MTU.<br>
     * <br>
     * Only works with characteristic that has NotifyAcquired<br>
     * which relies on notify Flag and no other client have<br>
     * called StartNotify.<br>
     * <br>
     * Notification are enabled during this procedure so<br>
     * StartNotify shall not be called, any notification<br>
     * will be dispatched via file descriptor therefore the<br>
     * Value property is not affected during the time where<br>
     * notify has been acquired.<br>
     * <br>
     * To release the lock the client shall close the file<br>
     * descriptor, a HUP is generated in case the device<br>
     * is disconnected.<br>
     * <br>
     * Note: the MTU can only be negotiated once and is<br>
     * symmetric therefore this method may be delayed in<br>
     * order to have the exchange MTU completed, because of<br>
     * that the file descriptor is closed during<br>
     * reconnections as the MTU has to be renegotiated.<br>
     * <br>
     * Possible options: "device": Object Device (Server only)<br>
     * 		  "MTU": Exchanged MTU (Server only)<br>
     * <br>
     *
     * @param _options
     *
     * @throws BluezFailedException
     * @throws BluezNotSupportedException
     */
    TwoTuple<FileDescriptor, UInt16> AcquireNotify(Map<?, ?> _options) throws BluezFailedException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Starts a notification session from this characteristic<br>
     * if it supports value notifications or indications.<br>
     * <br>
     *
     * @throws BluezFailedException
     * @throws BluezNotPermittedException
     * @throws BluezInProgressException
     * @throws BluezNotSupportedException
     */
    void StartNotify() throws BluezFailedException, BluezNotPermittedException, BluezInProgressException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method will cancel any previous StartNotify<br>
     * transaction. Note that notifications from a<br>
     * characteristic are shared between sessions thus<br>
     * calling StopNotify will release a single session.<br>
     * <br>
     *
     * @throws BluezFailedException
     */
    void StopNotify() throws BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method doesn't expect a reply so it is just a<br>
     * confirmation that value was received.<br>
     * <br>
     *
     * @throws BluezFailedException
     */
    void Confirm() throws BluezFailedException;

}
