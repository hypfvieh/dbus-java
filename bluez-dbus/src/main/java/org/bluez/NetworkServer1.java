package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: network-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.NetworkServer1<br>
 * <br>
 * <b>Object path:</b><br>
 *             /org/bluez/{hci0,hci1,...}<br>
 * <br>
 */
public interface NetworkServer1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Register server for the provided UUID. Every new<br>
     * connection to this server will be added the bridge<br>
     * interface.<br>
     * <br>
     * Valid UUIDs are "gn", "panu" or "nap".<br>
     * <br>
     * Initially no network server SDP is provided. Only<br>
     * after this method a SDP record will be available<br>
     * and the BNEP server will be ready for incoming<br>
     * connections.<br>
     * <br>
     * 
     * @param _uuid
     * @param _bridge
     */
    void Register(String _uuid, String _bridge);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Unregister the server for provided UUID.<br>
     * <br>
     * All servers will be automatically unregistered when<br>
     * the calling application terminates.<br>
     * 
     * @param _uuid
     */
    void Unregister(String _uuid);

}
