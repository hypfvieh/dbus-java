package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezAlreadyConnectedException;
import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezAuthenticationCanceledException;
import org.bluez.exceptions.BluezAuthenticationFailedException;
import org.bluez.exceptions.BluezAuthenticationRejectedException;
import org.bluez.exceptions.BluezAuthenticationTimeoutException;
import org.bluez.exceptions.BluezConnectionAttemptFailedException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAvailableException;
import org.bluez.exceptions.BluezNotConnectedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: device-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.Device1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX
 * 
 * Supported properties: 
 * 
 * 		string Address [readonly]
 * 
 * 			The Bluetooth device address of the remote device.
 * 
 * 		string AddressType [readonly]
 * 
 * 			The Bluetooth device Address Type. For dual-mode and
 * 			BR/EDR only devices this defaults to "public". Single
 * 			mode LE devices may have either value. If remote device
 * 			uses privacy than before pairing this represents address
 * 			type used for connection and Identity Address after
 * 			pairing.
 * 
 * 			Possible values:
 * 				"public" - Public address
 * 				"random" - Random address
 * 
 * 		string Name [readonly, optional]
 * 
 * 			The Bluetooth remote name. This value can not be
 * 			changed. Use the Alias property instead.
 * 
 * 			This value is only present for completeness. It is
 * 			better to always use the Alias property when
 * 			displaying the devices name.
 * 
 * 			If the Alias property is unset, it will reflect
 * 			this value which makes it more convenient.
 * 
 * 		string Icon [readonly, optional]
 * 
 * 			Proposed icon name according to the freedesktop.org
 * 			icon naming specification.
 * 
 * 		uint32 Class [readonly, optional]
 * 
 * 			The Bluetooth class of device of the remote device.
 * 
 * 		uint16 Appearance [readonly, optional]
 * 
 * 			External appearance of device, as found on GAP service.
 * 
 * 		array{string} UUIDs [readonly, optional]
 * 
 * 			List of 128-bit UUIDs that represents the available
 * 			remote services.
 * 
 * 		boolean Paired [readonly]
 * 
 * 			Indicates if the remote device is paired.
 * 
 * 		boolean Connected [readonly]
 * 
 * 			Indicates if the remote device is currently connected.
 * 			A PropertiesChanged signal indicate changes to this
 * 			status.
 * 
 * 		boolean Trusted [readwrite]
 * 
 * 			Indicates if the remote is seen as trusted. This
 * 			setting can be changed by the application.
 * 
 * 		boolean Blocked [readwrite]
 * 
 * 			If set to true any incoming connections from the
 * 			device will be immediately rejected. Any device
 * 			drivers will also be removed and no new ones will
 * 			be probed as long as the device is blocked.
 * 
 * 		string Alias [readwrite]
 * 
 * 			The name alias for the remote device. The alias can
 * 			be used to have a different friendly name for the
 * 			remote device.
 * 
 * 			In case no alias is set, it will return the remote
 * 			device name. Setting an empty string as alias will
 * 			convert it back to the remote device name.
 * 
 * 			When resetting the alias with an empty string, the
 * 			property will default back to the remote name.
 * 
 * 		object Adapter [readonly]
 * 
 * 			The object path of the adapter the device belongs to.
 * 
 * 		boolean LegacyPairing [readonly]
 * 
 * 			Set to true if the device only supports the pre-2.1
 * 			pairing mechanism. This property is useful during
 * 			device discovery to anticipate whether legacy or
 * 			simple pairing will occur if pairing is initiated.
 * 
 * 			Note that this property can exhibit false-positives
 * 			in the case of Bluetooth 2.1 (or newer) devices that
 * 			have disabled Extended Inquiry Response support.
 * 
 * 		string Modalias [readonly, optional]
 * 
 * 			Remote Device ID information in modalias format
 * 			used by the kernel and udev.
 * 
 * 		int16 RSSI [readonly, optional]
 * 
 * 			Received Signal Strength Indicator of the remote
 * 			device (inquiry or advertising).
 * 
 * 		int16 TxPower [readonly, optional]
 * 
 * 			Advertised transmitted power level (inquiry or
 * 			advertising).
 * 
 * 		dict ManufacturerData [readonly, optional]
 * 
 * 			Manufacturer specific advertisement data. Keys are
 * 			16 bits Manufacturer ID followed by its byte array
 * 			value.
 * 
 * 		dict ServiceData [readonly, optional]
 * 
 * 			Service advertisement data. Keys are the UUIDs in
 * 			string format followed by its byte array value.
 * 
 * 		bool ServicesResolved [readonly]
 * 
 * 			Indicate whether or not service discovery has been
 * 			resolved.
 * 
 * 		array{byte} AdvertisingFlags [readonly, experimental]
 * 
 * 			The Advertising Data Flags of the remote device.
 * 
 */
public interface Device1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This is a generic method to connect any profiles<br>
     * the remote device supports that can be connected<br>
     * to and have been flagged as auto-connectable on<br>
     * our side. If only subset of profiles is already<br>
     * connected it will try to connect currently disconnected<br>
     * ones.<br>
     * <br>
     * If at least one profile was connected successfully this<br>
     * method will indicate success.<br>
     * <br>
     * For dual-mode devices only one bearer is connected at<br>
     * time, the conditions are in the following order:<br>
     * <br>
     * 	1. Connect the disconnected bearer if already<br>
     * 	connected.<br>
     * <br>
     * 	2. Connect first the bonded bearer. If no<br>
     * 	bearers are bonded or both are skip and check<br>
     * 	latest seen bearer.<br>
     * <br>
     * 	3. Connect last seen bearer, in case the<br>
     * 	timestamps are the same BR/EDR takes<br>
     * 	precedence.<br>
     * <br>
     * 
     * @throws BluezNotReadyException
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezAlreadyConnectedException
     */
    void Connect() throws BluezNotReadyException, BluezFailedException, BluezInProgressException, BluezAlreadyConnectedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gracefully disconnects all connected<br>
     * profiles and then terminates low-level ACL connection.<br>
     * <br>
     * ACL connection will be terminated even if some profiles<br>
     * were not disconnected properly e.g. due to misbehaving<br>
     * device.<br>
     * <br>
     * This method can be also used to cancel a preceding<br>
     * Connect call before a reply to it has been received.<br>
     * <br>
     * For non-trusted devices connected over LE bearer calling<br>
     * this method will disable incoming connections until<br>
     * Connect method is called again.<br>
     * <br>
     * 
     * @throws BluezNotConnectedException
     */
    void Disconnect() throws BluezNotConnectedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method connects a specific profile of this<br>
     * device. The UUID provided is the remote service<br>
     * UUID for the profile.<br>
     * <br>
     * 
     * @param _uuid
     * 
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezInvalidArgumentsException
     * @throws BluezNotAvailableException
     * @throws BluezNotReadyException
     */
    void ConnectProfile(String _uuid) throws BluezFailedException, BluezInProgressException, BluezInvalidArgumentsException, BluezNotAvailableException, BluezNotReadyException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method disconnects a specific profile of<br>
     * this device. The profile needs to be registered<br>
     * client profile.<br>
     * <br>
     * There is no connection tracking for a profile, so<br>
     * as long as the profile is registered this will always<br>
     * succeed.<br>
     * <br>
     * 
     * @param _uuid
     * 
     * @throws BluezFailedException
     * @throws BluezInProgressException
     * @throws BluezInvalidArgumentsException
     * @throws BluezNotSupportedException
     */
    void DisconnectProfile(String _uuid) throws BluezFailedException, BluezInProgressException, BluezInvalidArgumentsException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method will connect to the remote device,<br>
     * initiate pairing and then retrieve all SDP records<br>
     * (or GATT primary services).<br>
     * <br>
     * If the application has registered its own agent,<br>
     * then that specific agent will be used. Otherwise<br>
     * it will use the default agent.<br>
     * <br>
     * Only for applications like a pairing wizard it<br>
     * would make sense to have its own agent. In almost<br>
     * all other cases the default agent will handle<br>
     * this just fine.<br>
     * <br>
     * In case there is no application agent and also<br>
     * no default agent present, this method will fail.<br>
     * <br>
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     * @throws BluezAlreadyExistsException
     * @throws BluezAuthenticationCanceledException
     * @throws BluezAuthenticationFailedException
     * @throws BluezAuthenticationRejectedException
     * @throws BluezAuthenticationTimeoutException
     * @throws BluezConnectionAttemptFailedException
     */
    void Pair() throws BluezInvalidArgumentsException, BluezFailedException, BluezAlreadyExistsException, BluezAuthenticationCanceledException, BluezAuthenticationFailedException, BluezAuthenticationRejectedException, BluezAuthenticationTimeoutException, BluezConnectionAttemptFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method can be used to cancel a pairing<br>
     * operation initiated by the Pair method.<br>
     * <br>
     * 
     * @throws BluezDoesNotExistException
     * @throws BluezFailedException
     */
    void CancelPairing() throws BluezDoesNotExistException, BluezFailedException;

}
