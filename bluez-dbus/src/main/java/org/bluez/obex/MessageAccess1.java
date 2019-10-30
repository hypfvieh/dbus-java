package org.bluez.obex;

import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: obex-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez.obex<br>
 * <b>Interface:</b> org.bluez.obex.MessageAccess1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [Session object path]<br>
 * <br>
 */
public interface MessageAccess1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Set working directory for current session, *name* may<br>
     * be the directory name or '..[/dir]'.<br>
     * <br>
     *
     * @param _name
     *
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    void SetFolder(String _name) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Returns a dictionary containing information about<br>
     * the current folder content.<br>
     * <br>
     * The following keys are defined:<br>
     * <br>
     * 	string Name : Folder name<br>
     * <br>
     * Possible filters: Offset and MaxCount<br>
     * <br>
     *
     * @param _filter
     *
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    Map<String, Variant<?>>[] ListFolders(Map<String, Variant<?>> _filter) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return all available fields that can be used in Fields<br>
     * filter.<br>
     * <br>
     */
    String[] ListFilterFields();

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Returns an array containing the messages found in the<br>
     * given subfolder of the current folder, or in the<br>
     * current folder if folder is empty.<br>
     * <br>
     * Possible Filters: Offset, MaxCount, SubjectLength, Fields,<br>
     * Type, PeriodStart, PeriodEnd, Status, Recipient, Sender,<br>
     * Priority<br>
     * <br>
     * Each message is represented by an object path followed<br>
     * by a dictionary of the properties.<br>
     * <br>
     * Properties:<br>
     * <br>
     * 	string Subject:<br>
     * <br>
     * 		Message subject<br>
     * <br>
     * 	string Timestamp:<br>
     * <br>
     * 		Message timestamp<br>
     * <br>
     * 	string Sender:<br>
     * <br>
     * 		Message sender name<br>
     * <br>
     * 	string SenderAddress:<br>
     * <br>
     * 		Message sender address<br>
     * <br>
     * 	string ReplyTo:<br>
     * <br>
     * 		Message Reply-To address<br>
     * <br>
     * 	string Recipient:<br>
     * <br>
     * 		Message recipient name<br>
     * <br>
     * 	string RecipientAddress:<br>
     * <br>
     * 		Message recipient address<br>
     * <br>
     * 	string Type:<br>
     * <br>
     * 		Message type<br>
     * <br>
     * 		Possible values: "email", "sms-gsm",<br>
     * 		"sms-cdma" and "mms"<br>
     * <br>
     * 	uint64 Size:<br>
     * <br>
     * 		Message size in bytes<br>
     * <br>
     * 	boolean Text:<br>
     * <br>
     * 		Message text flag<br>
     * <br>
     * 		Specifies whether message has textual<br>
     * 		content or is binary only<br>
     * <br>
     * 	string Status:<br>
     * <br>
     * 		Message status<br>
     * <br>
     * 		Possible values for received messages:<br>
     * 		"complete", "fractioned", "notification"<br>
     * <br>
     * 		Possible values for sent messages:<br>
     * 		"delivery-success", "sending-success",<br>
     * 		"delivery-failure", "sending-failure"<br>
     * <br>
     * 	uint64 AttachmentSize:<br>
     * <br>
     * 		Message overall attachment size in bytes<br>
     * <br>
     * 	boolean Priority:<br>
     * <br>
     * 		Message priority flag<br>
     * <br>
     * 	boolean Read:<br>
     * <br>
     * 		Message read flag<br>
     * <br>
     * 	boolean Sent:<br>
     * <br>
     * 		Message sent flag<br>
     * <br>
     * 	boolean Protected:<br>
     * <br>
     * 		Message protected flag<br>
     * <br>
     *
     * @param _folder
     * @param _filter
     *
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>>[] ListMessages(String _folder, Map<String, Variant<?>> _filter) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Request remote to update its inbox.<br>
     * <br>
     *
     * @throws BluezFailedException on failure
     */
    void UpdateInbox() throws BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Transfer a message (in bMessage format) to the<br>
     * remote device.<br>
     * <br>
     * The message is transferred either to the given<br>
     * subfolder of the current folder, or to the current<br>
     * folder if folder is empty.<br>
     * <br>
     * Possible args: Transparent, Retry, Charset<br>
     * <br>
     * The returned path represents the newly created transfer,<br>
     * which should be used to find out if the content has been<br>
     * successfully transferred or if the operation fails.<br>
     * <br>
     * The properties of this transfer are also returned along<br>
     * with the object path, to avoid a call to GetAll.<br>
     * <br>
     *
     * @param _sourcefile
     * @param _folder
     * @param _args
     *
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezFailedException on failure
     */
    TwoTuple<DBusPath, Map<String,Variant<?>>> PushMessage(String _sourcefile, String _folder, Map<String, Variant<?>> _args) throws BluezInvalidArgumentsException, BluezFailedException;

}
