package org.bluez;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: media-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez (Controller role)<br>
 * <b>Interface:</b> org.bluez.MediaPlayer1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/playerX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string Equalizer [readwrite]<br>
 * <br>
 * 			Possible values: "off" or "on"<br>
 * <br>
 * 		string Repeat [readwrite]<br>
 * <br>
 * 			Possible values: "off", "singletrack", "alltracks" or<br>
 * 					"group"<br>
 * <br>
 * 		string Shuffle [readwrite]<br>
 * <br>
 * 			Possible values: "off", "alltracks" or "group"<br>
 * <br>
 * 		string Scan [readwrite]<br>
 * <br>
 * 			Possible values: "off", "alltracks" or "group"<br>
 * <br>
 * 		string Status [readonly]<br>
 * <br>
 * 			Possible status: "playing", "stopped", "paused",<br>
 * 					"forward-seek", "reverse-seek"<br>
 * 					or "error"<br>
 * <br>
 * 		uint32 Position [readonly]<br>
 * <br>
 * 			Playback position in milliseconds. Changing the<br>
 * 			position may generate additional events that will be<br>
 * 			sent to the remote device. When position is 0 it means<br>
 * 			the track is starting and when it's greater than or<br>
 * 			equal to track's duration the track has ended. Note<br>
 * 			that even if duration is not available in metadata it's<br>
 * 			possible to signal its end by setting position to the<br>
 * 			maximum uint32 value.<br>
 * <br>
 * 		dict Track [readonly]<br>
 * <br>
 * 			Track metadata.<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				string Title:<br>
 * <br>
 * 					Track title name<br>
 * <br>
 * 				string Artist:<br>
 * <br>
 * 					Track artist name<br>
 * <br>
 * 				string Album:<br>
 * <br>
 * 					Track album name<br>
 * <br>
 * 				string Genre:<br>
 * <br>
 * 					Track genre name<br>
 * <br>
 * 				uint32 NumberOfTracks:<br>
 * <br>
 * 					Number of tracks in total<br>
 * <br>
 * 				uint32 TrackNumber:<br>
 * <br>
 * 					Track number<br>
 * <br>
 * 				uint32 Duration:<br>
 * <br>
 * 					Track duration in milliseconds<br>
 * <br>
 * 		object Device [readonly]<br>
 * <br>
 * 			Device object path.<br>
 * <br>
 * 		string Name [readonly]<br>
 * <br>
 * 			Player name<br>
 * <br>
 * 		string Type [readonly]<br>
 * <br>
 * 			Player type<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				"Audio"<br>
 * 				"Video"<br>
 * 				"Audio Broadcasting"<br>
 * 				"Video Broadcasting"<br>
 * <br>
 * 		string Subtype [readonly]<br>
 * <br>
 * 			Player subtype<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				"Audio Book"<br>
 * 				"Podcast"<br>
 * <br>
 * 		boolean Browsable [readonly]<br>
 * <br>
 * 			If present indicates the player can be browsed using<br>
 * 			MediaFolder interface.<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				True: Supported and active<br>
 * 				False: Supported but inactive<br>
 * <br>
 * 			Note: If supported but inactive clients can enable it<br>
 * 			by using MediaFolder interface but it might interfere<br>
 * 			in the playback of other players.<br>
 * <br>
 * <br>
 * 		boolean Searchable [readonly]<br>
 * <br>
 * 			If present indicates the player can be searched using<br>
 * 			MediaFolder interface.<br>
 * <br>
 * 			Possible values:<br>
 * <br>
 * 				True: Supported and active<br>
 * 				False: Supported but inactive<br>
 * <br>
 * 			Note: If supported but inactive clients can enable it<br>
 * 			by using MediaFolder interface but it might interfere<br>
 * 			in the playback of other players.<br>
 * <br>
 * 		object Playlist<br>
 * <br>
 * 			Playlist object path.<br>
 * <br>
 * <br>
 */
public interface MediaPlayer1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Resume playback.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Play() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Pause playback.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Pause() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Stop playback.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Stop() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Next item.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Next() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Previous item.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Previous() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Fast forward playback, this action is only stopped<br>
     * when another method in this interface is called.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void FastForward() throws BluezNotSupportedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Rewind playback, this action is only stopped<br>
     * when another method in this interface is called.<br>
     * <br>
     * 
     * @throws BluezNotSupportedException when operation not supported
     * @throws BluezFailedException on failure
     */
    void Rewind() throws BluezNotSupportedException, BluezFailedException;

}
