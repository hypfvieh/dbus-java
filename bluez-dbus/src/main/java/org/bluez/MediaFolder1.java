package org.bluez;

import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: media-api.txt.<br>
 * <br>
 * <b>Service:</b> unique name (Target role)<br>
 * <b>Interface:</b> org.bluez.MediaFolder1<br>
 * <br>
 * <b>Object path:</b><br>
 *             freely definable (Target role)<br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/playerX<br>
 *             (Controller role)<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		uint32 NumberOfItems [readonly]<br>
 * <br>
 * 			Number of items in the folder<br>
 * <br>
 * 		string Name [readonly]<br>
 * <br>
 * 			Folder name:<br>
 * <br>
 * 			Possible values:<br>
 * 				"/Filesystem/...": Filesystem scope<br>
 * 				"/NowPlaying/...": NowPlaying scope<br>
 * <br>
 * 			Note: /NowPlaying folder might not be listed if player<br>
 * 			is stopped, folders created by Search are virtual so<br>
 * 			once another Search is perform or the folder is<br>
 * 			changed using ChangeFolder it will no longer be listed.<br>
 * <br>
 * <br>
 * 			Offset of the first item.<br>
 * <br>
 * 			Default value: 0<br>
 * <br>
 * 		uint32 End:<br>
 * <br>
 * 			Offset of the last item.<br>
 * <br>
 * 			Default value: NumbeOfItems<br>
 * <br>
 * 		array{string} Attributes<br>
 * <br>
 * 			Item properties that should be included in the list.<br>
 * <br>
 * 			Possible Values:<br>
 * <br>
 * 				"title", "artist", "album", "genre",<br>
 * 				"number-of-tracks", "number", "duration"<br>
 * <br>
 * 			Default Value: All<br>
 * <br>
 * <br>
 */
public interface MediaFolder1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return a folder object containing the search result.<br>
     * <br>
     * To list the items found use the folder object returned<br>
     * and pass to ChangeFolder.<br>
     * <br>
     *
     * @param _value
     * @param _filter
     *
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    DBusPath Search(String _value, Map<String, Variant<?>> _filter) throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return a list of items found<br>
     * <br>
     *
     * @param _filter
     *
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Properties>[] ListItems(Map<String, Variant<?>> _filter) throws BluezInvalidArgumentsException, BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Change current folder.<br>
     * <br>
     * Note: By changing folder the items of previous folder<br>
     * might be destroyed and have to be listed again, the<br>
     * exception is NowPlaying folder which should be always<br>
     * present while the player is active.<br>
     * <br>
     *
     * @param _folder
     *
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void ChangeFolder(DBusPath _folder) throws BluezInvalidArgumentsException, BluezNotSupportedException, BluezFailedException;

}
