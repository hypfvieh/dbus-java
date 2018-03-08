package org.bluez.obex;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.util.Map;
import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: obex-api.txt.
 * 
 * Service: org.bluez.obex
 * Interface: org.bluez.obex.ObjectPush1
 * 
 * Object path: 
 *             [Session object path]
 * 
 */
public interface ObjectPush1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Send one local file to the remote device.<br>
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
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     */
    TwoTuple<Object,Map<?,?>> SendFile(String _sourcefile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Request the business card from a remote device and<br>
     * store it in the local file.<br>
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
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     */
    TwoTuple<Object,Map<?,?>> PullBusinessCard(String _targetfile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Push the client's business card to the remote device<br>
     * and then retrieve the remote business card and store<br>
     * it in a local file.<br>
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
     * @param _clientfile
     * @param _targetfile
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     */
    TwoTuple<Object,Map<?,?>> ExchangeBusinessCards(String _clientfile, String _targetfile) throws BluezInvalidArgumentsException, BluezFailedException;

}
