package org.bluez;

import java.io.FileDescriptor;
import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidOffsetException;
import org.bluez.exceptions.BluezInvalidValueLengthException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: gatt-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.GattCharacteristic1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/serviceXX/charYYYY<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string UUID [read-only]<br>
 * <br>
 * 			128-bit characteristic UUID.<br>
 * <br>
 * 		object Service [read-only]<br>
 * <br>
 * 			Object path of the GATT service the characteristic<br>
 * 			belongs to.<br>
 * <br>
 * 		array{byte} Value [read-only, optional]<br>
 * <br>
 * 			The cached value of the characteristic. This property<br>
 * 			gets updated only after a successful read request and<br>
 * 			when a notification or indication is received, upon<br>
 * 			which a PropertiesChanged signal will be emitted.<br>
 * <br>
 * 		boolean WriteAcquired [read-only, optional]<br>
 * <br>
 * 			True, if this characteristic has been acquired by any<br>
 * 			client using AcquireWrite.<br>
 * <br>
 * 			For client properties is ommited in case<br>
 * 			'write-without-response' flag is not set.<br>
 * <br>
 * 			For server the presence of this property indicates<br>
 * 			that AcquireWrite is supported.<br>
 * <br>
 * 		boolean NotifyAcquired [read-only, optional]<br>
 * <br>
 * 			True, if this characteristic has been acquired by any<br>
 * 			client using AcquireNotify.<br>
 * <br>
 * 			For client this properties is ommited in case 'notify'<br>
 * 			flag is not set.<br>
 * <br>
 * 			For server the presence of this property indicates<br>
 * 			that AcquireNotify is supported.<br>
 * <br>
 * 		boolean Notifying [read-only, optional]<br>
 * <br>
 * 			True, if notifications or indications on this<br>
 * 			characteristic are currently enabled.<br>
 * <br>
 * 		array{string} Flags [read-only]<br>
 * <br>
 * 			Defines how the characteristic value can be used. See<br>
 * 			Core spec "Table 3.5: Characteristic Properties bit<br>
 * 			field", and "Table 3.8: Characteristic Extended<br>
 * 			Properties bit field". Allowed values:<br>
 * <br>
 * 				"broadcast"<br>
 * 				"read"<br>
 * 				"write-without-response"<br>
 * 				"write"<br>
 * 				"notify"<br>
 * 				"indicate"<br>
 * 				"authenticated-signed-writes"<br>
 * 				"reliable-write"<br>
 * 				"writable-auxiliaries"<br>
 * 				"encrypt-read"<br>
 * 				"encrypt-write"<br>
 * 				"encrypt-authenticated-read"<br>
 * 				"encrypt-authenticated-write"<br>
 * 				"secure-read" (Server only)<br>
 * 				"secure-write" (Server only)<br>
 * 				"authorize"<br>
 * <br>
 * <br>
 */
public interface GattCharacteristic1 extends DBusInterface, Properties {

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
     * @throws BluezFailedException on failure
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezNotPermittedException
     * @throws BluezNotAuthorizedException when not authorized
     * @throws BluezInvalidOffsetException
     * @throws BluezNotSupportedException when operation not supported
     */
    byte[] ReadValue(Map<String, Variant<?>> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezNotAuthorizedException, BluezInvalidOffsetException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Issues a request to write the value of the<br>
     * characteristic.<br>
     * <br>
     * Possible options: "offset": Start offset<br>
     * 		  "device": Device path (Server only)<br>
     * 		  "link": Link type (Server only)<br>
     * 		  "prepare-authorize": boolean Is prepare request<br>
     * <br>
     *
     * @param _value
     * @param _options
     *
     * @throws BluezFailedException on failure
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezNotPermittedException
     * @throws BluezInvalidValueLengthException
     * @throws BluezNotAuthorizedException when not authorized
     * @throws BluezNotSupportedException when operation not supported
     */
    void WriteValue(byte[] _value, Map<String, Variant<?>> _options) throws BluezFailedException, BluezInProgressException, BluezNotPermittedException, BluezInvalidValueLengthException, BluezNotAuthorizedException, BluezNotSupportedException;

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
     * 		  "link": Link type (Server only)<br>
     * <br>
     *
     * @param _options
     *
     * @throws BluezFailedException on failure
     * @throws BluezNotSupportedException when operation not supported
     */
    TwoTuple<FileDescriptor, UInt16> AcquireWrite(Map<String, Variant<?>> _options) throws BluezFailedException, BluezNotSupportedException;

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
     * 		  "link": Link type (Server only)<br>
     * <br>
     *
     * @param _options
     *
     * @throws BluezFailedException on failure
     * @throws BluezNotSupportedException when operation not supported
     */
    TwoTuple<FileDescriptor, UInt16> AcquireNotify(Map<String, Variant<?>> _options) throws BluezFailedException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Starts a notification session from this characteristic<br>
     * if it supports value notifications or indications.<br>
     * <br>
     *
     * @throws BluezFailedException on failure
     * @throws BluezNotPermittedException
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezNotSupportedException when operation not supported
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
     * @throws BluezFailedException on failure
     */
    void StopNotify() throws BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method doesn't expect a reply so it is just a<br>
     * confirmation that value was received.<br>
     * <br>
     *
     * @throws BluezFailedException on failure
     */
    void Confirm() throws BluezFailedException;

}
