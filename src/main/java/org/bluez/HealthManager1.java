package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAllowedException;
import org.bluez.exceptions.BluezNotFoundException;

/**
 * File generated - 2018-03-08.<br>
 * Based on bluez Documentation: health-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.HealthManager1<br>
 * <br>
 * <b>Object path:</b><br>
 *             /org/bluez/<br>
 * <br>
 */
public interface HealthManager1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Returns the path of the new registered application.<br>
     * Application will be closed by the call or implicitly<br>
     * when the programs leaves the bus.<br>
     * <br>
     * config:<br>
     * 	uint16 DataType:<br>
     * <br>
     * 		Mandatory<br>
     * <br>
     * 	string Role:<br>
     * <br>
     * 		Mandatory. Possible values: "source",<br>
     * 			"sink"<br>
     * <br>
     * 	string Description:<br>
     * <br>
     * 		Optional<br>
     * <br>
     * 	ChannelType:<br>
     * <br>
     * 		Optional, just for sources. Possible<br>
     * 		values: "reliable", "streaming"<br>
     * <br>
     * 
     * @param _config
     * 
     * @throws BluezInvalidArgumentsException
     */
    Object CreateApplication(Map<?, ?> _config) throws BluezInvalidArgumentsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Closes the HDP application identified by the object<br>
     * path. Also application will be closed if the process<br>
     * that started it leaves the bus. Only the creator of the<br>
     * application will be able to destroy it.<br>
     * <br>
     * 
     * @param _application
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezNotFoundException
     * @throws BluezNotAllowedException
     */
    void DestroyApplication(Object _application) throws BluezInvalidArgumentsException, BluezNotFoundException, BluezNotAllowedException;

}
