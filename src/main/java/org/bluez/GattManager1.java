package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;
import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezInvalidArgumentsException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: gatt-api.txt.
 * 
 * Interface: org.bluez.GattManager1
 * Object path: [variable prefix]/{hci0,hci1,...}
 * 
 */
public interface GattManager1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Registers a local GATT services hierarchy as described<br>
     * above (GATT Server) and/or GATT profiles (GATT Client).<br>
     * <br>
     * The application object path together with the D-Bus<br>
     * system bus connection ID define the identification of<br>
     * the application registering a GATT based<br>
     * service or profile.<br>
     * <br>
     * 
     * @param _application
     * @param _options
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezAlreadyExistsException
     */
    void RegisterApplication(Object _application, Map<?, ?> _options) throws BluezInvalidArgumentsException, BluezAlreadyExistsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This unregisters the services that has been<br>
     * previously registered. The object path parameter<br>
     * must match the same value that has been used<br>
     * on registration.<br>
     * <br>
     * 
     * @param _application
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezDoesNotExistException
     */
    void UnregisterApplication(Object _application) throws BluezInvalidArgumentsException, BluezDoesNotExistException;

}
