package org.bluez;

import org.bluez.exceptions.BluezHealthErrorException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAllowedException;
import org.bluez.exceptions.BluezNotFoundException;
import org.bluez.exceptions.BluezOutOfRangeException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.DBusSignal;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: health-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.HealthDevice1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		object MainChannel [readonly]<br>
 * <br>
 * 			The first reliable channel opened. It is needed by<br>
 * 			upper applications in order to send specific protocol<br>
 * 			data units. The first reliable can change after a<br>
 * 			reconnection.<br>
 * <br>
 * <br>
 * <br>
 */
public interface HealthDevice1 extends DBusInterface, Properties {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Sends an echo petition to the remote service. Returns<br>
     * True if response matches with the buffer sent. If some<br>
     * error is detected False value is returned.<br>
     * <br>
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezOutOfRangeException when value is out of range
     */
    boolean Echo() throws BluezInvalidArgumentsException, BluezOutOfRangeException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Creates a new data channel.  The configuration should<br>
     * indicate the channel quality of service using one of<br>
     * this values "reliable", "streaming", "any".<br>
     * <br>
     * Returns the object path that identifies the data<br>
     * channel that is already connected.<br>
     * <br>
     * 
     * @param _application
     * @param _configuration
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezHealthErrorException when operation fails
     */
    DBusPath CreateChannel(DBusPath _application, String _configuration) throws BluezInvalidArgumentsException, BluezHealthErrorException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Destroys the data channel object. Only the creator of<br>
     * the channel or the creator of the HealthApplication<br>
     * that received the data channel will be able to destroy<br>
     * it.<br>
     * <br>
     * 
     * @param _channel
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezNotFoundException when item not found
     * @throws BluezNotAllowedException when operation not allowed
     */
    void DestroyChannel(DBusPath _channel) throws BluezInvalidArgumentsException, BluezNotFoundException, BluezNotAllowedException;

    /**
     * This signal is launched when a new data channel is<br>
     * created or when a known data channel is reconnected.<br>
     */
    public class ChannelConnected extends DBusSignal {
        private DBusPath channel;

        public ChannelConnected(String _path, DBusPath _channel) throws DBusException {
            super(_path, _channel);
            this.channel = _channel;
        }

        public DBusPath getChannel() {
            return channel;
        }
    }
    /**
     * This signal is launched when a data channel is deleted.<br>
     * After this signal the data channel path will not be<br>
     * valid and its path can be reused for future data<br>
     * channels.<br>
     */
    public class ChannelDeleted extends DBusSignal {
        private DBusPath channel;

        public ChannelDeleted(String _path, DBusPath _channel) throws DBusException {
            super(_path, _channel);
            this.channel = _channel;
        }

        public DBusPath getChannel() {
            return channel;
        }
    }
}
