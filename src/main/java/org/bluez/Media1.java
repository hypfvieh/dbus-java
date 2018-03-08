package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotSupportedException;

/**
 * File generated - 2018-03-08.<br>
 * Based on bluez Documentation: media-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.Media1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}<br>
 * <br>
 */
public interface Media1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Register a local end point to sender, the sender can<br>
     * register as many end points as it likes.<br>
     * <br>
     * Note: If the sender disconnects the end points are<br>
     * automatically unregistered.<br>
     * <br>
     * possible properties:<br>
     * <br>
     * 	string UUID:<br>
     * <br>
     * 		UUID of the profile which the endpoint<br>
     * 		is for.<br>
     * <br>
     * 	byte Codec:<br>
     * <br>
     * 		Assigned number of codec that the<br>
     * 		endpoint implements. The values should<br>
     * 		match the profile specification which<br>
     * 		is indicated by the UUID.<br>
     * <br>
     * 	array{byte} Capabilities:<br>
     * <br>
     * 		Capabilities blob, it is used as it is<br>
     * 		so the size and byte order must match.<br>
     * <br>
     * 
     * @param _endpoint
     * @param _properties
     * 
     * @throws BluezInvalidArgumentsException
     */
    void RegisterEndpoint(Object _endpoint, Map<?, ?> _properties) throws BluezInvalidArgumentsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Unregister sender end point.<br>
     * <br>
     * 
     * @param _endpoint
     */
    void UnregisterEndpoint(Object _endpoint);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Register a media player object to sender, the sender<br>
     * can register as many objects as it likes.<br>
     * <br>
     * Object must implement at least<br>
     * org.mpris.MediaPlayer2.Player as defined in MPRIS 2.2<br>
     * spec:<br>
     * <br>
     * http://specifications.freedesktop.org/mpris-spec/latest/<br>
     * <br>
     * Note: If the sender disconnects its objects are<br>
     * automatically unregistered.<br>
     * <br>
     * 
     * @param _player
     * @param _properties
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezNotSupportedException
     */
    void RegisterPlayer(Object _player, Map<?, ?> _properties) throws BluezInvalidArgumentsException, BluezNotSupportedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Unregister sender media player.<br>
     * <br>
     * <br>
     * 
     * @param _player
     */
    void UnregisterPlayer(Object _player);

}
