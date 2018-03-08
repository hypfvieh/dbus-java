package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotSupportedException;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: media-api.txt.
 * 
 * Service: org.bluez (Controller role)
 * Interface: org.bluez.MediaPlayer1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/playerX
 * 
 * Supported properties: 
 * 
 * 		string Equalizer [readwrite]
 * 
 * 			Possible values: "off" or "on"
 * 
 * 		string Repeat [readwrite]
 * 
 * 			Possible values: "off", "singletrack", "alltracks" or
 * 					"group"
 * 
 * 		string Shuffle [readwrite]
 * 
 * 			Possible values: "off", "alltracks" or "group"
 * 
 * 		string Scan [readwrite]
 * 
 * 			Possible values: "off", "alltracks" or "group"
 * 
 * 		string Status [readonly]
 * 
 * 			Possible status: "playing", "stopped", "paused",
 * 					"forward-seek", "reverse-seek"
 * 					or "error"
 * 
 * 		uint32 Position [readonly]
 * 
 * 			Playback position in milliseconds. Changing the
 * 			position may generate additional events that will be
 * 			sent to the remote device. When position is 0 it means
 * 			the track is starting and when it's greater than or
 * 			equal to track's duration the track has ended. Note
 * 			that even if duration is not available in metadata it's
 * 			possible to signal its end by setting position to the
 * 			maximum uint32 value.
 * 
 * 		dict Track [readonly]
 * 
 * 			Track metadata.
 * 
 * 			Possible values:
 * 
 * 				string Title:
 * 
 * 					Track title name
 * 
 * 				string Artist:
 * 
 * 					Track artist name
 * 
 * 				string Album:
 * 
 * 					Track album name
 * 
 * 				string Genre:
 * 
 * 					Track genre name
 * 
 * 				uint32 NumberOfTracks:
 * 
 * 					Number of tracks in total
 * 
 * 				uint32 TrackNumber:
 * 
 * 					Track number
 * 
 * 				uint32 Duration:
 * 
 * 					Track duration in milliseconds
 * 
 * 		object Device [readonly]
 * 
 * 			Device object path.
 * 
 * 		string Name [readonly]
 * 
 * 			Player name
 * 
 * 		string Type [readonly]
 * 
 * 			Player type
 * 
 * 			Possible values:
 * 
 * 				"Audio"
 * 				"Video"
 * 				"Audio Broadcasting"
 * 				"Video Broadcasting"
 * 
 * 		string Subtype [readonly]
 * 
 * 			Player subtype
 * 
 * 			Possible values:
 * 
 * 				"Audio Book"
 * 				"Podcast"
 * 
 * 		boolean Browsable [readonly]
 * 
 * 			If present indicates the player can be browsed using
 * 			MediaFolder interface.
 * 
 * 			Possible values:
 * 
 * 				True: Supported and active
 * 				False: Supported but inactive
 * 
 * 			Note: If supported but inactive clients can enable it
 * 			by using MediaFolder interface but it might interfere
 * 			in the playback of other players.
 * 
 * 
 * 		boolean Searchable [readonly]
 * 
 * 			If present indicates the player can be searched using
 * 			MediaFolder interface.
 * 
 * 			Possible values:
 * 
 * 				True: Supported and active
 * 				False: Supported but inactive
 * 
 * 			Note: If supported but inactive clients can enable it
 * 			by using MediaFolder interface but it might interfere
 * 			in the playback of other players.
 * 
 * 		object Playlist
 * 
 * 			Playlist object path.
 * 
 * 
 */
public interface MediaPlayer1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Resume playback.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Play() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Pause playback.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Pause() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Stop playback.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Stop() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Next item.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Next() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Previous item.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Previous() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Fast forward playback, this action is only stopped<br>
     * when another method in this interface is called.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void FastForward() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Rewind playback, this action is only stopped<br>
     * when another method in this interface is called.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    void Rewind() throws BluezNotSupportedException, BluezFailedException;

}
