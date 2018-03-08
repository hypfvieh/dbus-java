package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: adapter-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.Adapter1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}
 * 
 * Supported properties: 
 * 
 * 		string Address [readonly]
 * 
 * 			The Bluetooth device address.
 * 
 * 		string AddressType [readonly]
 * 
 * 			The Bluetooth  Address Type. For dual-mode and BR/EDR
 * 			only adapter this defaults to "public". Single mode LE
 * 			adapters may have either value. With privacy enabled
 * 			this contains type of Identity Address and not type of
 * 			address used for connection.
 * 
 * 			Possible values:
 * 				"public" - Public address
 * 				"random" - Random address
 * 
 * 		string Name [readonly]
 * 
 * 			The Bluetooth system name (pretty hostname).
 * 
 * 			This property is either a static system default
 * 			or controlled by an external daemon providing
 * 			access to the pretty hostname configuration.
 * 
 * 		string Alias [readwrite]
 * 
 * 			The Bluetooth friendly name. This value can be
 * 			changed.
 * 
 * 			In case no alias is set, it will return the system
 * 			provided name. Setting an empty string as alias will
 * 			convert it back to the system provided name.
 * 
 * 			When resetting the alias with an empty string, the
 * 			property will default back to system name.
 * 
 * 			On a well configured system, this property never
 * 			needs to be changed since it defaults to the system
 * 			name and provides the pretty hostname. Only if the
 * 			local name needs to be different from the pretty
 * 			hostname, this property should be used as last
 * 			resort.
 * 
 * 		uint32 Class [readonly]
 * 
 * 			The Bluetooth class of device.
 * 
 * 			This property represents the value that is either
 * 			automatically configured by DMI/ACPI information
 * 			or provided as static configuration.
 * 
 * 		boolean Powered [readwrite]
 * 
 * 			Switch an adapter on or off. This will also set the
 * 			appropriate connectable state of the controller.
 * 
 * 			The value of this property is not persistent. After
 * 			restart or unplugging of the adapter it will reset
 * 			back to false.
 * 
 * 		boolean Discoverable [readwrite]
 * 
 * 			Switch an adapter to discoverable or non-discoverable
 * 			to either make it visible or hide it. This is a global
 * 			setting and should only be used by the settings
 * 			application.
 * 
 * 			If the DiscoverableTimeout is set to a non-zero
 * 			value then the system will set this value back to
 * 			false after the timer expired.
 * 
 * 			In case the adapter is switched off, setting this
 * 			value will fail.
 * 
 * 			When changing the Powered property the new state of
 * 			this property will be updated via a PropertiesChanged
 * 			signal.
 * 
 * 			For any new adapter this settings defaults to false.
 * 
 * 		boolean Pairable [readwrite]
 * 
 * 			Switch an adapter to pairable or non-pairable. This is
 * 			a global setting and should only be used by the
 * 			settings application.
 * 
 * 			Note that this property only affects incoming pairing
 * 			requests.
 * 
 * 			For any new adapter this settings defaults to true.
 * 
 * 		uint32 PairableTimeout [readwrite]
 * 
 * 			The pairable timeout in seconds. A value of zero
 * 			means that the timeout is disabled and it will stay in
 * 			pairable mode forever.
 * 
 * 			The default value for pairable timeout should be
 * 			disabled (value 0).
 * 
 * 		uint32 DiscoverableTimeout [readwrite]
 * 
 * 			The discoverable timeout in seconds. A value of zero
 * 			means that the timeout is disabled and it will stay in
 * 			discoverable/limited mode forever.
 * 
 * 			The default value for the discoverable timeout should
 * 			be 180 seconds (3 minutes).
 * 
 * 		boolean Discovering [readonly]
 * 
 * 			Indicates that a device discovery procedure is active.
 * 
 * 		array{string} UUIDs [readonly]
 * 
 * 			List of 128-bit UUIDs that represents the available
 * 			local services.
 * 
 * 		string Modalias [readonly, optional]
 * 
 * 			Local Device ID information in modalias format
 * 			used by the kernel and udev.
 * 
 */
public interface Adapter1 extends DBusInterface {

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
     * @throws BluezNotReadyException
     * @throws BluezFailedException
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
     * @throws BluezNotReadyException
     * @throws BluezFailedException
     * @throws BluezNotAuthorizedException
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
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     */
    void RemoveDevice(Object _device) throws BluezInvalidArgumentsException, BluezFailedException;

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
     * @throws BluezNotReadyException
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void SetDiscoveryFilter(Map<?, ?> _filter) throws BluezNotReadyException, BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return available filters that can be given to<br>
     * SetDiscoveryFilter.<br>
     * <br>
     */
    String[] GetDiscoveryFilters();

}
