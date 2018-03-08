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
 * Interface: org.bluez.obex.Message1
 * 
 * Object path: 
 *             [Session object path]/{message0,...}
 * 
 * Supported properties: 
 * 
 * 		string Folder [readonly]
 * 
 * 			Folder which the message belongs to
 * 
 * 		string Subject [readonly]
 * 
 * 			Message subject
 * 
 * 		string Timestamp [readonly]
 * 
 * 			Message timestamp
 * 
 * 		string Sender [readonly]
 * 
 * 			Message sender name
 * 
 * 		string SenderAddress [readonly]
 * 
 * 			Message sender address
 * 
 * 		string ReplyTo [readonly]
 * 
 * 			Message Reply-To address
 * 
 * 		string Recipient [readonly]
 * 
 * 			Message recipient name
 * 
 * 		string RecipientAddress [readonly]
 * 
 * 			Message recipient address
 * 
 * 		string Type [readonly]
 * 
 * 			Message type
 * 
 * 			Possible values: "email", "sms-gsm",
 * 			"sms-cdma" and "mms"
 * 
 * 		uint64 Size [readonly]
 * 
 * 			Message size in bytes
 * 
 * 		string Status [readonly]
 * 
 * 			Message reception status
 * 
 * 			Possible values: "complete",
 * 			"fractioned" and "notification"
 * 
 * 		boolean Priority [readonly]
 * 
 * 			Message priority flag
 * 
 * 		boolean Read [read/write]
 * 
 * 			Message read flag
 * 
 * 		boolean Deleted [writeonly]
 * 
 * 			Message deleted flag
 * 
 * 		boolean Sent [readonly]
 * 
 * 			Message sent flag
 * 
 * 		boolean Protected [readonly]
 * 
 * 			Message protected flag
 * 
 */
public interface Message1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Download message and store it in the target file.<br>
     * <br>
     * If an empty target file is given, a temporary file<br>
     * will be automatically generated.<br>
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
     * @param _attachment
     * 
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     */
    TwoTuple<Object,Map<?,?>> Get(String _targetfile, boolean _attachment) throws BluezInvalidArgumentsException, BluezFailedException;

}
