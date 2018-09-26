package org.bluez.obex;

import org.bluez.exceptions.BluezCanceledException;
import org.bluez.exceptions.BluezRejectedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-agent-api.txt.<br>
 * <br>
 * <b>Service:</b> unique name<br>
 * <b>Interface:</b> org.bluez.obex.Agent1<br>
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
     * needs to accept/reject a Bluetooth object push request.<br>
     * <br>
     * Returns the full path (including the filename) where<br>
     * the object shall be stored. The tranfer object will<br>
     * contain a Filename property that contains the default<br>
     * location and name that can be returned.<br>
     * <br>
     * 
     * @param _transfer
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    String AuthorizePush(DBusPath _transfer) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called to indicate that the agent<br>
     * request failed before a reply was returned. It cancels<br>
     * the previous request.<br>
     */
    void Cancel();

}
