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
 * <b>Interface:</b> org.bluez.obex.FileTransfer<br>
 * <br>
 * <b>Object path:</b><br>
 *             [Session object path]<br>
 * <br>
 */
public interface FileTransfer extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Change the current folder of the remote device.<br>
     * <br>
     * 
     * @param _folder
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void ChangeFolder(String _folder) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Create a new folder in the remote device.<br>
     * <br>
     * 
     * @param _folder
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void CreateFolder(String _folder) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Returns a dictionary containing information about<br>
     * the current folder content.<br>
     * <br>
     * The following keys are defined:<br>
     * <br>
     * 	string Name : Object name in UTF-8 format<br>
     * 	string Type : Either "folder" or "file"<br>
     * 	uint64 Size : Object size or number of items in<br>
     * folder<br>
     * 	string Permission : Group, owner and other<br>
     * 	permission<br>
     * 	uint64 Modified : Last change<br>
     * 	uint64 Accessed : Last access<br>
     * 	uint64 Created : Creation date<br>
     * <br>
     * 
     * @throws BluezFailedException on failure
     */
    Map<String, Variant<?>>[] ListFolder() throws BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Copy the source file (from remote device) to the<br>
     * target file (on local filesystem).<br>
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
     * @param _sourcefile
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>> GetFile(String _targetfile, String _sourcefile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Copy the source file (from local filesystem) to the<br>
     * target file (on remote device).<br>
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
     * @param _targetfile
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>> PutFile(String _sourcefile, String _targetfile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Copy a file within the remote device from source file<br>
     * to target file.<br>
     * <br>
     * 
     * @param _sourcefile
     * @param _targetfile
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void CopyFile(String _sourcefile, String _targetfile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Move a file within the remote device from source file<br>
     * to the target file.<br>
     * <br>
     * 
     * @param _sourcefile
     * @param _targetfile
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void MoveFile(String _sourcefile, String _targetfile) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Deletes the specified file/folder.<br>
     * <br>
     * 
     * @param _file
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void Delete(String _file) throws BluezInvalidArgumentsException, BluezFailedException;

}
