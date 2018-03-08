package org.bluez;

import java.util.Map;
import java.util.Properties;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: media-api.txt.
 *
 * Service: unique name (Target role)
 * Interface: org.bluez.MediaFolder1
 *
 * Object path:
 *             freely definable (Target role)
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/playerX
 *             (Controller role)
 *
 * Supported properties:
 *
 * 		uint32 NumberOfItems [readonly]
 *
 * 			Number of items in the folder
 *
 * 		string Name [readonly]
 *
 * 			Folder name:
 *
 * 			Possible values:
 * 				"/Filesystem/...": Filesystem scope
 * 				"/NowPlaying/...": NowPlaying scope
 *
 * 			Note: /NowPlaying folder might not be listed if player
 * 			is stopped, folders created by Search are virtual so
 * 			once another Search is perform or the folder is
 * 			changed using ChangeFolder it will no longer be listed.
 *
 *
 * 			Offset of the first item.
 *
 * 			Default value: 0
 *
 * 		uint32 End:
 *
 * 			Offset of the last item.
 *
 * 			Default value: NumbeOfItems
 *
 * 		array{string} Attributes
 *
 * 			Item properties that should be included in the list.
 *
 * 			Possible Values:
 *
 * 				"title", "artist", "album", "genre",
 * 				"number-of-tracks", "number", "duration"
 *
 * 			Default Value: All
 *
 *
 */
public interface MediaFolder1 extends DBusInterface {

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
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    Object Search(String _value, Map<?, ?> _filter) throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return a list of items found<br>
     * <br>
     *
     * @param _filter
     *
     * @throws BluezInvalidArgumentsException
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    TwoTuple<Object, Properties>[] ListItems(Map<?, ?> _filter) throws BluezInvalidArgumentsException, BluezNotSupportedException, BluezFailedException;

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
     * @throws BluezInvalidArgumentsException
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void ChangeFolder(Object _folder) throws BluezInvalidArgumentsException, BluezNotSupportedException, BluezFailedException;

}
