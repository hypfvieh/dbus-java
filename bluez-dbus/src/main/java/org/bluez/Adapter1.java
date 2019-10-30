package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: adapter-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Adapter1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string Address [readonly]<br>
 * <br>
 * 			The Bluetooth device address.<br>
 * <br>
 * 		string AddressType [readonly]<br>
 * <br>
 * 			The Bluetooth  Address Type. For dual-mode and BR/EDR<br>
 * 			only adapter this defaults to "public". Single mode LE<br>
 * 			adapters may have either value. With privacy enabled<br>
 * 			this contains type of Identity Address and not type of<br>
 * 			address used for connection.<br>
 * <br>
 * 			Possible values:<br>
 * 				"public" - Public address<br>
 * 				"random" - Random address<br>
 * <br>
 * 		string Name [readonly]<br>
 * <br>
 * 			The Bluetooth system name (pretty hostname).<br>
 * <br>
 * 			This property is either a static system default<br>
 * 			or controlled by an external daemon providing<br>
 * 			access to the pretty hostname configuration.<br>
 * <br>
 * 		string Alias [readwrite]<br>
 * <br>
 * 			The Bluetooth friendly name. This value can be<br>
 * 			changed.<br>
 * <br>
 * 			In case no alias is set, it will return the system<br>
 * 			provided name. Setting an empty string as alias will<br>
 * 			convert it back to the system provided name.<br>
 * <br>
 * 			When resetting the alias with an empty string, the<br>
 * 			property will default back to system name.<br>
 * <br>
 * 			On a well configured system, this property never<br>
 * 			needs to be changed since it defaults to the system<br>
 * 			name and provides the pretty hostname. Only if the<br>
 * 			local name needs to be different from the pretty<br>
 * 			hostname, this property should be used as last<br>
 * 			resort.<br>
 * <br>
 * 		uint32 Class [readonly]<br>
 * <br>
 * 			The Bluetooth class of device.<br>
 * <br>
 * 			This property represents the value that is either<br>
 * 			automatically configured by DMI/ACPI information<br>
 * 			or provided as static configuration.<br>
 * <br>
 * 		boolean Powered [readwrite]<br>
 * <br>
 * 			Switch an adapter on or off. This will also set the<br>
 * 			appropriate connectable state of the controller.<br>
 * <br>
 * 			The value of this property is not persistent. After<br>
 * 			restart or unplugging of the adapter it will reset<br>
 * 			back to false.<br>
 * <br>
 * 		boolean Discoverable [readwrite]<br>
 * <br>
 * 			Switch an adapter to discoverable or non-discoverable<br>
 * 			to either make it visible or hide it. This is a global<br>
 * 			setting and should only be used by the settings<br>
 * 			application.<br>
 * <br>
 * 			If the DiscoverableTimeout is set to a non-zero<br>
 * 			value then the system will set this value back to<br>
 * 			false after the timer expired.<br>
 * <br>
 * 			In case the adapter is switched off, setting this<br>
 * 			value will fail.<br>
 * <br>
 * 			When changing the Powered property the new state of<br>
 * 			this property will be updated via a PropertiesChanged<br>
 * 			signal.<br>
 * <br>
 * 			For any new adapter this settings defaults to false.<br>
 * <br>
 * 		boolean Pairable [readwrite]<br>
 * <br>
 * 			Switch an adapter to pairable or non-pairable. This is<br>
 * 			a global setting and should only be used by the<br>
 * 			settings application.<br>
 * <br>
 * 			Note that this property only affects incoming pairing<br>
 * 			requests.<br>
 * <br>
 * 			For any new adapter this settings defaults to true.<br>
 * <br>
 * 		uint32 PairableTimeout [readwrite]<br>
 * <br>
 * 			The pairable timeout in seconds. A value of zero<br>
 * 			means that the timeout is disabled and it will stay in<br>
 * 			pairable mode forever.<br>
 * <br>
 * 			The default value for pairable timeout should be<br>
 * 			disabled (value 0).<br>
 * <br>
 * 		uint32 DiscoverableTimeout [readwrite]<br>
 * <br>
 * 			The discoverable timeout in seconds. A value of zero<br>
 * 			means that the timeout is disabled and it will stay in<br>
 * 			discoverable/limited mode forever.<br>
 * <br>
 * 			The default value for the discoverable timeout should<br>
 * 			be 180 seconds (3 minutes).<br>
 * <br>
 * 		boolean Discovering [readonly]<br>
 * <br>
 * 			Indicates that a device discovery procedure is active.<br>
 * <br>
 * 		array{string} UUIDs [readonly]<br>
 * <br>
 * 			List of 128-bit UUIDs that represents the available<br>
 * 			local services.<br>
 * <br>
 * 		string Modalias [readonly, optional]<br>
 * <br>
 * 			Local Device ID information in modalias format<br>
 * 			used by the kernel and udev.<br>
 * <br>
 */
public interface Adapter1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method starts the device discovery session. This<br>
     * includes an inquiry procedure and remote device name<br>
     * resolving. Use StopDiscovery to release the sessions<br>
     * acquired.<br>
     * <br>
     * This process will start creating Device objects as<br>
     * new devices are discovered.<br>
     * <br>
     * During discovery RSSI delta-threshold is imposed.<br>
     * <br>
     * 
     * @throws BluezNotReadyException when bluez not ready
     * @throws BluezFailedException on failure
     */
    void StartDiscovery() throws BluezNotReadyException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method will cancel any previous StartDiscovery<br>
     * transaction.<br>
     * <br>
     * Note that a discovery procedure is shared between all<br>
     * discovery sessions thus calling StopDiscovery will only<br>
     * release a single session.<br>
     * <br>
     * 
     * @throws BluezNotReadyException when bluez not ready
     * @throws BluezFailedException on failure
     * @throws BluezNotAuthorizedException when not authorized
     */
    void StopDiscovery() throws BluezNotReadyException, BluezFailedException, BluezNotAuthorizedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This removes the remote device object at the given<br>
     * path. It will remove also the pairing information.<br>
     * <br>
     * 
     * @param _device
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void RemoveDevice(DBusPath _device) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method sets the device discovery filter for the<br>
     * caller. When this method is called with no filter<br>
     * parameter, filter is removed.<br>
     * <br>
     * Parameters that may be set in the filter dictionary<br>
     * include the following:<br>
     * <br>
     * array{string} UUIDs<br>
     * <br>
     * 	Filter by service UUIDs, empty means match<br>
     * 	_any_ UUID.<br>
     * <br>
     * 	When a remote device is found that advertises<br>
     * 	any UUID from UUIDs, it will be reported if:<br>
     * 	- Pathloss and RSSI are both empty.<br>
     * 	- only Pathloss param is set, device advertise<br>
     * 	  TX pwer, and computed pathloss is less than<br>
     * 	  Pathloss param.<br>
     * 	- only RSSI param is set, and received RSSI is<br>
     * 	  higher than RSSI param.<br>
     * <br>
     * int16 RSSI<br>
     * <br>
     * 	RSSI threshold value.<br>
     * <br>
     * 	PropertiesChanged signals will be emitted<br>
     * 	for already existing Device objects, with<br>
     * 	updated RSSI value. If one or more discovery<br>
     * 	filters have been set, the RSSI delta-threshold,<br>
     * 	that is imposed by StartDiscovery by default,<br>
     * 	will not be applied.<br>
     * <br>
     * uint16 Pathloss<br>
     * <br>
     * 	Pathloss threshold value.<br>
     * <br>
     * 	PropertiesChanged signals will be emitted<br>
     * 	for already existing Device objects, with<br>
     * 	updated Pathloss value.<br>
     * <br>
     * string Transport (Default "auto")<br>
     * <br>
     * 	Transport parameter determines the type of<br>
     * 	scan.<br>
     * <br>
     * 	Possible values:<br>
     * 		"auto"	- interleaved scan<br>
     * 		"bredr"	- BR/EDR inquiry<br>
     * 		"le"	- LE scan only<br>
     * <br>
     * 	If "le" or "bredr" Transport is requested,<br>
     * 	and the controller doesn't support it,<br>
     * 	org.bluez.Error.Failed error will be returned.<br>
     * 	If "auto" transport is requested, scan will use<br>
     * 	LE, BREDR, or both, depending on what's<br>
     * 	currently enabled on the controller.<br>
     * <br>
     * bool DuplicateData (Default: true)<br>
     * <br>
     * 	Disables duplicate detection of advertisement<br>
     * 	data.<br>
     * <br>
     * 	When enabled PropertiesChanged signals will be<br>
     * 	generated for either ManufacturerData and<br>
     * 	ServiceData everytime they are discovered.<br>
     * <br>
     * When discovery filter is set, Device objects will be<br>
     * created as new devices with matching criteria are<br>
     * discovered regardless of they are connectable or<br>
     * discoverable which enables listening to<br>
     * non-connectable and non-discoverable devices.<br>
     * <br>
     * When multiple clients call SetDiscoveryFilter, their<br>
     * filters are internally merged, and notifications about<br>
     * new devices are sent to all clients. Therefore, each<br>
     * client must check that device updates actually match<br>
     * its filter.<br>
     * <br>
     * When SetDiscoveryFilter is called multiple times by the<br>
     * same client, last filter passed will be active for<br>
     * given client.<br>
     * <br>
     * SetDiscoveryFilter can be called before StartDiscovery.<br>
     * It is useful when client will create first discovery<br>
     * session, to ensure that proper scan will be started<br>
     * right after call to StartDiscovery.<br>
     * <br>
     * 
     * @param _filter
     * 
     * @throws BluezNotReadyException when bluez not ready
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void SetDiscoveryFilter(Map<String, Variant<?>> _filter) throws BluezNotReadyException, BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return available filters that can be given to<br>
     * SetDiscoveryFilter.<br>
     * <br>
     */
    String[] GetDiscoveryFilters();

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method connects to device without need of<br>
     * performing General Discovery. Connection mechanism is<br>
     * similar to Connect method from Device1 interface with<br>
     * exception that this method returns success when physical<br>
     * connection is established. After this method returns,<br>
     * services discovery will continue and any supported<br>
     * profile will be connected. There is no need for calling<br>
     * Connect on Device1 after this call. If connection was<br>
     * successful this method returns object path to created<br>
     * device object.<br>
     * <br>
     * Parameters that may be set in the filter dictionary<br>
     * include the following:<br>
     * <br>
     * string Address<br>
     * <br>
     * 	The Bluetooth device address of the remote<br>
     * 	device. This parameter is mandatory.<br>
     * <br>
     * string AddressType<br>
     * <br>
     * 	The Bluetooth device Address Type. This is<br>
     * 	address type that should be used for initial<br>
     * 	connection. If this parameter is not present<br>
     * 	BR/EDR device is created.<br>
     * <br>
     * 	Possible values:<br>
     * 		"public" - Public address<br>
     * 		"random" - Random address<br>
     * <br>
     * 
     * @param _properties
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezAlreadyExistsException when item already exists
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezNotReadyException when bluez not ready
     * @throws BluezFailedException on failure
     */
    DBusPath ConnectDevice(Map<String, Variant<?>> _properties) throws BluezInvalidArgumentsException, BluezAlreadyExistsException, BluezNotSupportedException, BluezNotReadyException, BluezFailedException;

}
