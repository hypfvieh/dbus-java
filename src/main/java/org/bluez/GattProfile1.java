package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: gatt-api.txt.<br>
 * <br>
 * <b>Service:</b> &lt;application dependent&gt;<br>
 * <b>Interface:</b> org.bluez.GattProfile1<br>
 * <br>
 * <b>Object path:</b><br>
 *             &lt;application dependent&gt;<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		array{string} UUIDs [read-only]<br>
 * <br>
 * 			128-bit GATT service UUIDs to auto connect.<br>
 * <br>
 * <br>
 * <br>
 */
public interface GattProfile1 extends DBusInterface, Properties {

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
