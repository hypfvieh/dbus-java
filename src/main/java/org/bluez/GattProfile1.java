package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: gatt-api.txt.
 * 
 * Service: <application dependent>
 * Interface: org.bluez.GattProfile1
 * 
 * Object path: 
 *             <application dependent>
 * 
 * Supported properties: 
 * 
 * 		array{string} UUIDs [read-only]
 * 
 * 			128-bit GATT service UUIDs to auto connect.
 * 
 * 
 * 
 */
public interface GattProfile1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * unregisters the profile. The profile can use it to<br>
     * do cleanup tasks. There is no need to unregister the<br>
     * profile, because when this method gets called it has<br>
     * already been unregistered.<br>
     * <br>
     */
    void Release();

}
