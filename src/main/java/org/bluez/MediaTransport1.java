package org.bluez;

import org.freedesktop.dbus.interfaces.DBusInterface;
import java.io.FileDescriptor;
import org.bluez.datatypes.ThreeTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotAvailableException;
import org.freedesktop.dbus.types.UInt16;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: media-api.txt.
 * 
 * Service: org.bluez
 * Interface: org.bluez.MediaTransport1
 * 
 * Object path: 
 *             [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX/fdX
 * 
 * Supported properties: 
 * 
 * 		object Device [readonly]
 * 
 * 			Device object which the transport is connected to.
 * 
 * 		string UUID [readonly]
 * 
 * 			UUID of the profile which the transport is for.
 * 
 * 		byte Codec [readonly]
 * 
 * 			Assigned number of codec that the transport support.
 * 			The values should match the profile specification which
 * 			is indicated by the UUID.
 * 
 * 		array{byte} Configuration [readonly]
 * 
 * 			Configuration blob, it is used as it is so the size and
 * 			byte order must match.
 * 
 * 		string State [readonly]
 * 
 * 			Indicates the state of the transport. Possible
 * 			values are:
 * 				"idle": not streaming
 * 				"pending": streaming but not acquired
 * 				"active": streaming and acquired
 * 
 * 		uint16 Delay [readwrite]
 * 
 * 			Optional. Transport delay in 1/10 of millisecond, this
 * 			property is only writeable when the transport was
 * 			acquired by the sender.
 * 
 * 		uint16 Volume [readwrite]
 * 
 * 			Optional. Indicates volume level of the transport,
 * 			this property is only writeable when the transport was
 * 			acquired by the sender.
 * 
 * 			Possible Values: 0-127
 * 
 */
public interface MediaTransport1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Acquire transport file descriptor and the MTU for read<br>
     * and write respectively.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException
     * @throws BluezFailedException
     */
    ThreeTuple<FileDescriptor, UInt16, UInt16> Acquire() throws BluezNotAuthorizedException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Acquire transport file descriptor only if the transport<br>
     * is in "pending" state at the time the message is<br>
     * received by BlueZ. Otherwise no request will be sent<br>
     * to the remote device and the function will just fail<br>
     * with org.bluez.Error.NotAvailable.<br>
     * <br>
     * 
     * @throws BluezNotAuthorizedException
     * @throws BluezFailedException
     * @throws BluezNotAvailableException
     */
    ThreeTuple<FileDescriptor, UInt16, UInt16> TryAcquire() throws BluezNotAuthorizedException, BluezFailedException, BluezNotAvailableException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Releases file descriptor.<br>
     * <br>
     */
    void Release();

}
