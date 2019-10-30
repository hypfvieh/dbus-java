package org.bluez;

import org.bluez.exceptions.BluezCanceledException;
import org.bluez.exceptions.BluezRejectedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: agent-api.txt.<br>
 * <br>
 * <b>Service:</b> unique name<br>
 * <b>Interface:</b> org.bluez.Agent1<br>
 * <br>
 * <b>Object path:</b><br>
 *             freely definable<br>
 * <br>
 */
public interface Agent1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * unregisters the agent. An agent can use it to do<br>
     * cleanup tasks. There is no need to unregister the<br>
     * agent, because when this method gets called it has<br>
     * already been unregistered.<br>
     * <br>
     */
    void Release();

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * needs to get the passkey for an authentication.<br>
     * <br>
     * The return value should be a string of 1-16 characters<br>
     * length. The string can be alphanumeric.<br>
     * <br>
     * 
     * @param _device
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    String RequestPinCode(DBusPath _device) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * needs to display a pincode for an authentication.<br>
     * <br>
     * An empty reply should be returned. When the pincode<br>
     * needs no longer to be displayed, the Cancel method<br>
     * of the agent will be called.<br>
     * <br>
     * This is used during the pairing process of keyboards<br>
     * that don't support Bluetooth 2.1 Secure Simple Pairing,<br>
     * in contrast to DisplayPasskey which is used for those<br>
     * that do.<br>
     * <br>
     * This method will only ever be called once since<br>
     * older keyboards do not support typing notification.<br>
     * <br>
     * Note that the PIN will always be a 6-digit number,<br>
     * zero-padded to 6 digits. This is for harmony with<br>
     * the later specification.<br>
     * <br>
     * 
     * @param _device
     * @param _pincode
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    void DisplayPinCode(DBusPath _device, String _pincode) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * needs to get the passkey for an authentication.<br>
     * <br>
     * The return value should be a numeric value<br>
     * between 0-999999.<br>
     * <br>
     * 
     * @param _device
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    UInt32 RequestPasskey(DBusPath _device) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * needs to display a passkey for an authentication.<br>
     * <br>
     * The entered parameter indicates the number of already<br>
     * typed keys on the remote side.<br>
     * <br>
     * An empty reply should be returned. When the passkey<br>
     * needs no longer to be displayed, the Cancel method<br>
     * of the agent will be called.<br>
     * <br>
     * During the pairing process this method might be<br>
     * called multiple times to update the entered value.<br>
     * <br>
     * Note that the passkey will always be a 6-digit number,<br>
     * so the display should be zero-padded at the start if<br>
     * the value contains less than 6 digits.<br>
     * <br>
     * 
     * @param _device
     * @param _passkey
     * @param _entered
     */
    void DisplayPasskey(DBusPath _device, UInt32 _passkey, UInt16 _entered);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * needs to confirm a passkey for an authentication.<br>
     * <br>
     * To confirm the value it should return an empty reply<br>
     * or an error in case the passkey is invalid.<br>
     * <br>
     * Note that the passkey will always be a 6-digit number,<br>
     * so the display should be zero-padded at the start if<br>
     * the value contains less than 6 digits.<br>
     * <br>
     * 
     * @param _device
     * @param _passkey
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    void RequestConfirmation(DBusPath _device, UInt32 _passkey) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called to request the user to<br>
     * authorize an incoming pairing attempt which<br>
     * would in other circumstances trigger the just-works<br>
     * model, or when the user plugged in a device that<br>
     * implements cable pairing. In the latter case, the<br>
     * device would not be connected to the adapter via<br>
     * Bluetooth yet.<br>
     * <br>
     * 
     * @param _device
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    void RequestAuthorization(DBusPath _device) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * needs to authorize a connection/service request.<br>
     * <br>
     * 
     * @param _device
     * @param _uuid
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    void AuthorizeService(DBusPath _device, String _uuid) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called to indicate that the agent<br>
     * request failed before a reply was returned.<br>
     */
    void Cancel();

}
