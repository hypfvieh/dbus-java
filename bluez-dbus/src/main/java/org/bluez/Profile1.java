package org.bluez;

import java.io.FileDescriptor;
import java.util.Map;

import org.bluez.exceptions.BluezCanceledException;
import org.bluez.exceptions.BluezRejectedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: profile-api.txt.<br>
 * <br>
 * <b>Service:</b> unique name<br>
 * <b>Interface:</b> org.bluez.Profile1<br>
 * <br>
 * <b>Object path:</b><br>
 *             freely definable<br>
 * <br>
 */
public interface Profile1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * unregisters the profile. A profile can use it to do<br>
     * cleanup tasks. There is no need to unregister the<br>
     * profile, because when this method gets called it has<br>
     * already been unregistered.<br>
     * <br>
     */
    void Release();

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when a new service level<br>
     * connection has been made and authorized.<br>
     * <br>
     * Common fd_properties:<br>
     * <br>
     * uint16 Version		Profile version (optional)<br>
     * uint16 Features		Profile features (optional)<br>
     * <br>
     * 
     * @param _device
     * @param fd
     * @param _fd_properties
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    void NewConnection(DBusPath _device, FileDescriptor fd, Map<String, Variant<?>> _fd_properties) throws BluezRejectedException, BluezCanceledException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when a profile gets<br>
     * disconnected.<br>
     * <br>
     * The file descriptor is no longer owned by the service<br>
     * daemon and the profile implementation needs to take<br>
     * care of cleaning up all connections.<br>
     * <br>
     * If multiple file descriptors are indicated via<br>
     * NewConnection, it is expected that all of them<br>
     * are disconnected before returning from this<br>
     * method call.<br>
     * <br>
     * 
     * @param _device
     * 
     * @throws BluezRejectedException when operation rejected
     * @throws BluezCanceledException when operation canceled
     */
    void RequestDisconnection(DBusPath _device) throws BluezRejectedException, BluezCanceledException;

}
