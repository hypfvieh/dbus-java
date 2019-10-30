package org.bluez.obex;

import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez.obex<br>
 * <b>Interface:</b> org.bluez.obex.Message1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [Session object path]/{message0,...}<br>
 * <br>
 * <b>Supported properties:</b> <br>
 * <br>
 * 		string Folder [readonly]<br>
 * <br>
 * 			Folder which the message belongs to<br>
 * <br>
 * 		string Subject [readonly]<br>
 * <br>
 * 			Message subject<br>
 * <br>
 * 		string Timestamp [readonly]<br>
 * <br>
 * 			Message timestamp<br>
 * <br>
 * 		string Sender [readonly]<br>
 * <br>
 * 			Message sender name<br>
 * <br>
 * 		string SenderAddress [readonly]<br>
 * <br>
 * 			Message sender address<br>
 * <br>
 * 		string ReplyTo [readonly]<br>
 * <br>
 * 			Message Reply-To address<br>
 * <br>
 * 		string Recipient [readonly]<br>
 * <br>
 * 			Message recipient name<br>
 * <br>
 * 		string RecipientAddress [readonly]<br>
 * <br>
 * 			Message recipient address<br>
 * <br>
 * 		string Type [readonly]<br>
 * <br>
 * 			Message type<br>
 * <br>
 * 			Possible values: "email", "sms-gsm",<br>
 * 			"sms-cdma" and "mms"<br>
 * <br>
 * 		uint64 Size [readonly]<br>
 * <br>
 * 			Message size in bytes<br>
 * <br>
 * 		string Status [readonly]<br>
 * <br>
 * 			Message reception status<br>
 * <br>
 * 			Possible values: "complete",<br>
 * 			"fractioned" and "notification"<br>
 * <br>
 * 		boolean Priority [readonly]<br>
 * <br>
 * 			Message priority flag<br>
 * <br>
 * 		boolean Read [read/write]<br>
 * <br>
 * 			Message read flag<br>
 * <br>
 * 		boolean Deleted [writeonly]<br>
 * <br>
 * 			Message deleted flag<br>
 * <br>
 * 		boolean Sent [readonly]<br>
 * <br>
 * 			Message sent flag<br>
 * <br>
 * 		boolean Protected [readonly]<br>
 * <br>
 * 			Message protected flag<br>
 * <br>
 */
public interface Message1 extends DBusInterface, Properties {

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
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>> Get(String _targetfile, boolean _attachment) throws BluezInvalidArgumentsException, BluezFailedException;

}
