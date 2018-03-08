package org.bluez.obex;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotInProgressException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: obex-api.txt.
 * 
 * Service: org.bluez.obex
 * Interface: org.bluez.obex.Transfer1
 * 
 * Object path: 
 *             [Session object path]/transfer{0, 1, 2, ...}
 * 
 * Supported properties: 
 * 
 * 		string Status [readonly]
 * 
 * 			Inform the current status of the transfer.
 * 
 * 			Possible values: "queued", "active", "suspended",
 * 					"complete" or "error"
 * 
 * 		object Session [readonly]
 * 
 * 			The object path of the session the transfer belongs
 * 			to.
 * 
 * 		string Name [readonly]
 * 
 * 			Name of the transferred object. Either Name or Type
 * 			or both will be present.
 * 
 * 		string Type [readonly]
 * 
 * 			Type of the transferred object. Either Name or Type
 * 			or both will be present.
 * 
 * 		uint64 Time [readonly, optional]
 * 
 * 			Time of the transferred object if this is
 * 			provided by the remote party.
 * 
 * 		uint64 Size [readonly, optional]
 * 
 * 			Size of the transferred object. If the size is
 * 			unknown, then this property will not be present.
 * 
 * 		uint64 Transferred [readonly, optional]
 * 
 * 			Number of bytes transferred. For queued transfers, this
 * 			value will not be present.
 * 
 * 		string Filename [readonly, optional]
 * 
 * 			Complete name of the file being received or sent.
 * 
 * 			For incoming object push transaction, this will be
 * 			the proposed default location and name. It can be
 * 			overwritten by the AuthorizePush agent callback
 * 			and will be then updated accordingly.
 * 
 * 
 * 
 */
public interface Transfer1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Stops the current transference.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException
     * @throws BluezInProgressException
     * @throws BluezFailedException
     */
    void Cancel() throws BluezNotAuthorizedException, BluezInProgressException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Suspend transference.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException
     * @throws BluezNotInProgressException
     */
    void Suspend() throws BluezNotAuthorizedException, BluezNotInProgressException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Resume transference.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException
     * @throws BluezNotInProgressException
     */
    void Resume() throws BluezNotAuthorizedException, BluezNotInProgressException;

}
