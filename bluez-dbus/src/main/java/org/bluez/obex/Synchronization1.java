package org.bluez.obex;

import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez.obex<br>
 * <b>Interface:</b> org.bluez.obex.Synchronization1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [Session object path]<br>
 * <br>
 */
public interface Synchronization1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Set the phonebook object store location for other<br>
     * operations. Should be called before all the other<br>
     * operations.<br>
     * <br>
     * location: Where the phonebook is stored, possible<br>
     * values:<br>
     * 	"int" ( "internal" which is default )<br>
     * 	"sim1"<br>
     * 	"sim2"<br>
     * 	......<br>
     * <br>
     * 
     * @param _location
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     */
    void SetLocation(String _location) throws BluezInvalidArgumentsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Retrieve an entire Phonebook Object store from remote<br>
     * device, and stores it in a local file.<br>
     * <br>
     * If an empty target file is given, a name will be<br>
     * automatically calculated for the temporary file.<br>
     * <br>
     * The returned path represents the newly created transfer,<br>
     * which should be used to find out if the content has been<br>
     * successfully transferred or if the operation fails.<br>
     * <br>
     * The properties of this transfer are also returned along<br>
     * with the object path, to avoid a call to GetProperties.<br>
     * <br>
     * 
     * @param _targetfile
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>> GetPhonebook(String _targetfile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Send an entire Phonebook Object store to remote device.<br>
     * <br>
     * The returned path represents the newly created transfer,<br>
     * which should be used to find out if the content has been<br>
     * successfully transferred or if the operation fails.<br>
     * <br>
     * The properties of this transfer are also returned along<br>
     * with the object path, to avoid a call to GetProperties.<br>
     * <br>
     * 
     * @param _sourcefile
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>> PutPhonebook(String _sourcefile) throws BluezInvalidArgumentsException, BluezFailedException;

}
