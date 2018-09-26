package org.bluez.obex;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotInProgressException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez.obex<br>
 * <b>Interface:</b> org.bluez.obex.Transfer1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [Session object path]/transfer{0, 1, 2, ...}<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string Status [readonly]<br>
 * <br>
 * 			Inform the current status of the transfer.<br>
 * <br>
 * 			Possible values: "queued", "active", "suspended",<br>
 * 					"complete" or "error"<br>
 * <br>
 * 		object Session [readonly]<br>
 * <br>
 * 			The object path of the session the transfer belongs<br>
 * 			to.<br>
 * <br>
 * 		string Name [readonly]<br>
 * <br>
 * 			Name of the transferred object. Either Name or Type<br>
 * 			or both will be present.<br>
 * <br>
 * 		string Type [readonly]<br>
 * <br>
 * 			Type of the transferred object. Either Name or Type<br>
 * 			or both will be present.<br>
 * <br>
 * 		uint64 Time [readonly, optional]<br>
 * <br>
 * 			Time of the transferred object if this is<br>
 * 			provided by the remote party.<br>
 * <br>
 * 		uint64 Size [readonly, optional]<br>
 * <br>
 * 			Size of the transferred object. If the size is<br>
 * 			unknown, then this property will not be present.<br>
 * <br>
 * 		uint64 Transferred [readonly, optional]<br>
 * <br>
 * 			Number of bytes transferred. For queued transfers, this<br>
 * 			value will not be present.<br>
 * <br>
 * 		string Filename [readonly, optional]<br>
 * <br>
 * 			Complete name of the file being received or sent.<br>
 * <br>
 * 			For incoming object push transaction, this will be<br>
 * 			the proposed default location and name. It can be<br>
 * 			overwritten by the AuthorizePush agent callback<br>
 * 			and will be then updated accordingly.<br>
 * <br>
 * <br>
 * <br>
 */
public interface Transfer1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Stops the current transference.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException when not authorized
     * @throws BluezInProgressException when operation already in progress
     * @throws BluezFailedException on failure
     */
    void Cancel() throws BluezNotAuthorizedException, BluezInProgressException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Suspend transference.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException when not authorized
     * @throws BluezNotInProgressException
     */
    void Suspend() throws BluezNotAuthorizedException, BluezNotInProgressException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Resume transference.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException when not authorized
     * @throws BluezNotInProgressException
     */
    void Resume() throws BluezNotAuthorizedException, BluezNotInProgressException;

}
