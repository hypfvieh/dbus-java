package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: advertising-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.LEAdvertisement1
 * 
 * Object path: 
 *             freely definable
 * 
 * Supported properties: 
 * 
 * 		string Type
 * 
 * 			Determines the type of advertising packet requested.
 * 
 * 			Possible values: "broadcast" or "peripheral"
 * 
 * 		array{string} ServiceUUIDs
 * 
 * 			List of UUIDs to include in the "Service UUID" field of
 * 			the Advertising Data.
 * 
 * 		dict ManufacturerData
 * 
 * 			Manufactuer Data fields to include in
 * 			the Advertising Data.  Keys are the Manufacturer ID
 * 			to associate with the data.
 * 
 * 		array{string} SolicitUUIDs
 * 
 * 			Array of UUIDs to include in "Service Solicitation"
 * 			Advertisement Data.
 * 
 * 		dict ServiceData
 * 
 * 			Service Data elements to include. The keys are the
 * 			UUID to associate with the data.
 * 
 * 		array{string} Includes
 * 
 * 			List of features to be included in the advertising
 * 			packet.
 * 
 * 			Possible values: as found on
 * 					LEAdvertisingManager.SupportedIncludes
 * 
 * 		string LocalName
 * 
 * 			Local name to be used in the advertising report. If the
 * 			string is too big to fit into the packet it will be
 * 			truncated.
 * 
 * 			If this property is available 'local-name' cannot be
 * 			present in the Includes.
 * 
 * 		uint16 Appearance
 * 
 * 			Appearance to be used in the advertising report.
 * 
 * 			Possible values: as found on GAP Service.
 * 
 * 		uint16_t Duration
 * 
 * 			Duration of the advertisement in seconds. If there are
 * 			other applications advertising no duration is set the
 * 			default is 2 seconds.
 * 
 * 		uint16_t Timeout
 * 
 * 			Timeout of the advertisement in seconds. This defines
 * 			the lifetime of the advertisement.
 * 
 * 
 */
public interface LEAdvertisement1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * removes the Advertisement. A client can use it to do<br>
     * cleanup tasks. There is no need to call<br>
     * UnregisterAdvertisement because when this method gets<br>
     * called it has already been unregistered.<br>
     * <br>
     */
    void Release();

}
