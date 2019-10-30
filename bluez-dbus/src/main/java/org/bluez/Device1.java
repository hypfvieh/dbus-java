package org.bluez;

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
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: device-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Device1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string Address [readonly]<br>
 * <br>
 * 			The Bluetooth device address of the remote device.<br>
 * <br>
 * 		string AddressType [readonly]<br>
 * <br>
 * 			The Bluetooth device Address Type. For dual-mode and<br>
 * 			BR/EDR only devices this defaults to "public". Single<br>
 * 			mode LE devices may have either value. If remote device<br>
 * 			uses privacy than before pairing this represents address<br>
 * 			type used for connection and Identity Address after<br>
 * 			pairing.<br>
 * <br>
 * 			Possible values:<br>
 * 				"public" - Public address<br>
 * 				"random" - Random address<br>
 * <br>
 * 		string Name [readonly, optional]<br>
 * <br>
 * 			The Bluetooth remote name. This value can not be<br>
 * 			changed. Use the Alias property instead.<br>
 * <br>
 * 			This value is only present for completeness. It is<br>
 * 			better to always use the Alias property when<br>
 * 			displaying the devices name.<br>
 * <br>
 * 			If the Alias property is unset, it will reflect<br>
 * 			this value which makes it more convenient.<br>
 * <br>
 * 		string Icon [readonly, optional]<br>
 * <br>
 * 			Proposed icon name according to the freedesktop.org<br>
 * 			icon naming specification.<br>
 * <br>
 * 		uint32 Class [readonly, optional]<br>
 * <br>
 * 			The Bluetooth class of device of the remote device.<br>
 * <br>
 * 		uint16 Appearance [readonly, optional]<br>
 * <br>
 * 			External appearance of device, as found on GAP service.<br>
 * <br>
 * 		array{string} UUIDs [readonly, optional]<br>
 * <br>
 * 			List of 128-bit UUIDs that represents the available<br>
 * 			remote services.<br>
 * <br>
 * 		boolean Paired [readonly]<br>
 * <br>
 * 			Indicates if the remote device is paired.<br>
 * <br>
 * 		boolean Connected [readonly]<br>
 * <br>
 * 			Indicates if the remote device is currently connected.<br>
 * 			A PropertiesChanged signal indicate changes to this<br>
 * 			status.<br>
 * <br>
 * 		boolean Trusted [readwrite]<br>
 * <br>
 * 			Indicates if the remote is seen as trusted. This<br>
 * 			setting can be changed by the application.<br>
 * <br>
 * 		boolean Blocked [readwrite]<br>
 * <br>
 * 			If set to true any incoming connections from the<br>
 * 			device will be immediately rejected. Any device<br>
 * 			drivers will also be removed and no new ones will<br>
 * 			be probed as long as the device is blocked.<br>
 * <br>
 * 		string Alias [readwrite]<br>
 * <br>
 * 			The name alias for the remote device. The alias can<br>
 * 			be used to have a different friendly name for the<br>
 * 			remote device.<br>
 * <br>
 * 			In case no alias is set, it will return the remote<br>
 * 			device name. Setting an empty string as alias will<br>
 * 			convert it back to the remote device name.<br>
 * <br>
 * 			When resetting the alias with an empty string, the<br>
 * 			property will default back to the remote name.<br>
 * <br>
 * 		object Adapter [readonly]<br>
 * <br>
 * 			The object path of the adapter the device belongs to.<br>
 * <br>
 * 		boolean LegacyPairing [readonly]<br>
 * <br>
 * 			Set to true if the device only supports the pre-2.1<br>
 * 			pairing mechanism. This property is useful during<br>
 * 			device discovery to anticipate whether legacy or<br>
 * 			simple pairing will occur if pairing is initiated.<br>
 * <br>
 * 			Note that this property can exhibit false-positives<br>
 * 			in the case of Bluetooth 2.1 (or newer) devices that<br>
 * 			have disabled Extended Inquiry Response support.<br>
 * <br>
 * 		string Modalias [readonly, optional]<br>
 * <br>
 * 			Remote Device ID information in modalias format<br>
 * 			used by the kernel and udev.<br>
 * <br>
 * 		int16 RSSI [readonly, optional]<br>
 * <br>
 * 			Received Signal Strength Indicator of the remote<br>
 * 			device (inquiry or advertising).<br>
 * <br>
 * 		int16 TxPower [readonly, optional]<br>
 * <br>
 * 			Advertised transmitted power level (inquiry or<br>
 * 			advertising).<br>
 * <br>
 * 		dict ManufacturerData [readonly, optional]<br>
 * <br>
 * 			Manufacturer specific advertisement data. Keys are<br>
 * 			16 bits Manufacturer ID followed by its byte array<br>
 * 			value.<br>
 * <br>
 * 		dict ServiceData [readonly, optional]<br>
 * <br>
 * 			Service advertisement data. Keys are the UUIDs in<br>
 * 			string format followed by its byte array value.<br>
 * <br>
 * 		bool ServicesResolved [readonly]<br>
 * <br>
 * 			Indicate whether or not service discovery has been<br>
 * 			resolved.<br>
 * <br>
 * 		array{byte} AdvertisingFlags [readonly, experimental]<br>
 * <br>
 * 			The Advertising Data Flags of the remote device.<br>
 * <br>
 * 		dict AdvertisingData [readonly, experimental]<br>
 * <br>
 * 			The Advertising Data of the remote device. Keys are<br>
 * 			are 8 bits AD Type followed by data as byte array.<br>
 * <br>
 * 			Note: Only types considered safe to be handled by<br>
 * 			application are exposed.<br>
 * <br>
 * 			Possible values:<br>
 * 				&lt;type&gt; &lt;byte array&gt;<br>
 * 				...<br>
 * <br>
 * 			Example:<br>
 * 				&lt;Transport Discovery&gt; &lt;Organization Flags...&gt;<br>
 * 				0x26                   0x01         0x01...<br>
 * <br>
 */
public interface Device1 extends DBusInterface, Properties {

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
     * @throws BluezNotReadyException when bluez not ready
     * @throws BluezFailedException on failure
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezAlreadyConnectedException when already connected
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
     * @throws BluezNotConnectedException when bluez not connected
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
     * @throws BluezFailedException on failure
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezNotAvailableException when not available
     * @throws BluezNotReadyException when bluez not ready
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
     * @throws BluezFailedException on failure
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezNotSupportedException when operation not supported
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
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     * @throws BluezAlreadyExistsException when item already exists
     * @throws BluezAuthenticationCanceledException
     * @throws BluezAuthenticationFailedException when authentication failed
     * @throws BluezAuthenticationRejectedException
     * @throws BluezAuthenticationTimeoutException when authentication timed out
     * @throws BluezConnectionAttemptFailedException when connection attempt failed
     */
    void Pair() throws BluezInvalidArgumentsException, BluezFailedException, BluezAlreadyExistsException, BluezAuthenticationCanceledException, BluezAuthenticationFailedException, BluezAuthenticationRejectedException, BluezAuthenticationTimeoutException, BluezConnectionAttemptFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method can be used to cancel a pairing<br>
     * operation initiated by the Pair method.<br>
     * <br>
     * 
     * @throws BluezDoesNotExistException when item does not exist
     * @throws BluezFailedException on failure
     */
    void CancelPairing() throws BluezDoesNotExistException, BluezFailedException;

}
