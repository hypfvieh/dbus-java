package org.bluez.obex;

import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-agent-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez.obex<br>
 * <b>Interface:</b> org.bluez.obex.AgentManager1<br>
 * <br>
 * <b>Object path:</b><br>
 *             /org/bluez/obex<br>
 * <br>
 */
public interface AgentManager1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Register an agent to request authorization of<br>
     * the user to accept/reject objects. Object push<br>
     * service needs to authorize each received object.<br>
     * <br>
     * 
     * @param _agent
     * 
     * @throws BluezAlreadyExistsException when item already exists
     */
    void RegisterAgent(DBusPath _agent) throws BluezAlreadyExistsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This unregisters the agent that has been previously<br>
     * registered. The object path parameter must match the<br>
     * same value that has been used on registration.<br>
     * <br>
     * 
     * @param _agent
     * 
     * @throws BluezDoesNotExistException when item does not exist
     */
    void UnregisterAgent(DBusPath _agent) throws BluezDoesNotExistException;

}
