package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: media-api.txt.
 * 
 * Service: unique name
 * Interface: org.bluez.MediaEndpoint1
 * 
 * Object path: 
 *             freely definable
 * 
 */
public interface MediaEndpoint1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Set configuration for the transport.<br>
     * <br>
     * 
     * @param _transport
     * @param _properties
     */
    void SetConfiguration(Object _transport, Map<?, ?> _properties);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Select preferable configuration from the supported<br>
     * capabilities.<br>
     * <br>
     * Returns a configuration which can be used to setup<br>
     * a transport.<br>
     * <br>
     * Note: There is no need to cache the selected<br>
     * configuration since on success the configuration is<br>
     * send back as parameter of SetConfiguration.<br>
     * <br>
     * 
     * @param _capabilities
     */
    byte[] SelectConfiguration(byte[] _capabilities);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Clear transport configuration.<br>
     * <br>
     * 
     * @param _transport
     */
    void ClearConfiguration(Object _transport);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This method gets called when the service daemon<br>
     * unregisters the endpoint. An endpoint can use it to do<br>
     * cleanup tasks. There is no need to unregister the<br>
     * endpoint, because when this method gets called it has<br>
     * already been unregistered.<br>
     * <br>
     * <br>
     */
    void Release();

}
