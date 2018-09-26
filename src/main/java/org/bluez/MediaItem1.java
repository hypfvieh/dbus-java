package org.bluez;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: media-api.txt.<br>
 * <br>
 * <b>Service:</b> unique name (Target role)<br>
 * <b>Interface:</b> org.bluez.MediaItem1<br>
 * <br>
 * <b>Object path:</b><br>
 *             freely definable (Target role)<br>
 *             [variable<br>
 *             prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/playerX/itemX<br>
 *             (Controller role)<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		object Player [readonly]<br>
 * <br>
 * 			Player object path the item belongs to<br>
 * <br>
 * 		string Name [readonly]<br>
 * <br>
 * 			Item displayable name<br>
 * <br>
 * 		string Type [readonly]<br>
 * <br>
 * 			Item type<br>
 * <br>
 * 			Possible values: "video", "audio", "folder"<br>
 * <br>
 * 		string FolderType [readonly, optional]<br>
 * <br>
 * 			Folder type.<br>
 * <br>
 * 			Possible values: "mixed", "titles", "albums", "artists"<br>
 * <br>
 * 			Available if property Type is "Folder"<br>
 * <br>
 * 		boolean Playable [readonly, optional]<br>
 * <br>
 * 			Indicates if the item can be played<br>
 * <br>
 * 			Available if property Type is "folder"<br>
 * <br>
 * 		dict Metadata [readonly]<br>
 * <br>
 * 			Item metadata.<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				string Title<br>
 * <br>
 * 					Item title name<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * 				string Artist<br>
 * <br>
 * 					Item artist name<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * 				string Album<br>
 * <br>
 * 					Item album name<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * 				string Genre<br>
 * <br>
 * 					Item genre name<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * 				uint32 NumberOfTracks<br>
 * <br>
 * 					Item album number of tracks in total<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * 				uint32 Number<br>
 * <br>
 * 					Item album number<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * 				uint32 Duration<br>
 * <br>
 * 					Item duration in milliseconds<br>
 * <br>
 * 					Available if property Type is "audio"<br>
 * 					or "video"<br>
 * <br>
 * <br>
 */
public interface MediaItem1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Play item<br>
     * <br>
     *
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Play() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Add item to now playing list<br>
     * <br>
     *
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void AddtoNowPlaying() throws BluezNotSupportedException, BluezFailedException;

}
