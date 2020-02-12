package org.bluez;

import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotFoundException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2020-02-12.<br>
 * Based on bluez Documentation: thermometer-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.ThermometerManager1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}<br>
 * <br>
 */
public interface ThermometerManager1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Registers a watcher to monitor scanned measurements.<br>
     * This agent will be notified about final temperature<br>
     * measurements.<br>
     * <br>
     * 
     * @param _agent
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     */
    void RegisterWatcher(DBusPath _agent) throws BluezInvalidArgumentsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Unregisters a watcher.<br>
     * <br>
     * 
     * @param _agent
     */
    void UnregisterWatcher(DBusPath _agent);

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Enables intermediate measurement notifications<br>
     * for this agent. Intermediate measurements will<br>
     * be enabled only for thermometers which support it.<br>
     * <br>
     * 
     * @param _agent
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     */
    void EnableIntermediateMeasurement(DBusPath _agent) throws BluezInvalidArgumentsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Disables intermediate measurement notifications<br>
     * for this agent. It will disable notifications in<br>
     * thermometers when the last agent removes the<br>
     * watcher for intermediate measurements.<br>
     * <br>
     * 
     * @param _agent
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezNotFoundException when item not found
     */
    void DisableIntermediateMeasurement(DBusPath _agent) throws BluezInvalidArgumentsException, BluezNotFoundException;

}
