package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotSupportedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: media-api.txt.
 * 
 * Service: unique name (Target role)
 * Interface: org.bluez.MediaItem1
 * 
 * Object path: 
 *             freely definable (Target role)
 *             [variable
 *             prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/playerX/itemX
 *             (Controller role)
 * 
 * Supported properties: 
 * 
 * 		object Player [readonly]
 * 
 * 			Player object path the item belongs to
 * 
 * 		string Name [readonly]
 * 
 * 			Item displayable name
 * 
 * 		string Type [readonly]
 * 
 * 			Item type
 * 
 * 			Possible values: "video", "audio", "folder"
 * 
 * 		string FolderType [readonly, optional]
 * 
 * 			Folder type.
 * 
 * 			Possible values: "mixed", "titles", "albums", "artists"
 * 
 * 			Available if property Type is "Folder"
 * 
 * 		boolean Playable [readonly, optional]
 * 
 * 			Indicates if the item can be played
 * 
 * 			Available if property Type is "folder"
 * 
 * 		dict Metadata [readonly]
 * 
 * 			Item metadata.
 * 
 * 			Possible values:
 * 
 * 				string Title
 * 
 * 					Item title name
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 				string Artist
 * 
 * 					Item artist name
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 				string Album
 * 
 * 					Item album name
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 				string Genre
 * 
 * 					Item genre name
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 				uint32 NumberOfTracks
 * 
 * 					Item album number of tracks in total
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 				uint32 Number
 * 
 * 					Item album number
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 				uint32 Duration
 * 
 * 					Item duration in milliseconds
 * 
 * 					Available if property Type is "audio"
 * 					or "video"
 * 
 * 
 */
public interface MediaItem1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Play item<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Play() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Add item to now playing list<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void AddtoNowPlaying() throws BluezNotSupportedException, BluezFailedException;

}
