package org.bluez.obex;

import java.util.Map;

import org.bluez.datatypes.TwoTuple;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezForbiddenException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;

/**
 * File generated - 2018-03-08.
 * Based on bluez Documentation: obex-api.txt.
 *
 * Service: org.bluez.obex
 * Interface: org.bluez.obex.PhonebookAccess1
 *
 * Object path:
 *             [Session object path]
 *
 * Supported properties:
 *
 * 		string Folder [readonly]
 *
 * 			Current folder.
 *
 * 		string DatabaseIdentifier [readonly, optional]
 *
 * 			128 bits persistent database identifier.
 *
 * 			Possible values: 32-character hexadecimal such
 * 			as A1A2A3A4B1B2C1C2D1D2E1E2E3E4E5E6
 *
 * 		string PrimaryCounter [readonly, optional]
 *
 * 			128 bits primary version counter.
 *
 * 			Possible values: 32-character hexadecimal such
 * 			as A1A2A3A4B1B2C1C2D1D2E1E2E3E4E5E6
 *
 * 		string SecondaryCounter [readonly, optional]
 *
 * 			128 bits secondary version counter.
 *
 * 			Possible values: 32-character hexadecimal such
 * 			as A1A2A3A4B1B2C1C2D1D2E1E2E3E4E5E6
 *
 * 		bool FixedImageSize [readonly, optional]
 *
 * 			Indicate support for fixed image size.
 *
 * 			Possible values: True if image is JPEG 300x300 pixels
 * 			otherwise False.
 *
 *
 */
public interface PhonebookAccess1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Select the phonebook object for other operations. Should<br>
     * be call before all the other operations.<br>
     * <br>
     * location : Where the phonebook is stored, possible<br>
     * inputs :<br>
     * 	"int" ( "internal" which is default )<br>
     * 	"sim" ( "sim1" )<br>
     * 	"sim2"<br>
     * 	...<br>
     * <br>
     * phonebook : Possible inputs :<br>
     * 	"pb" :	phonebook for the saved contacts<br>
     * 	"ich":	incoming call history<br>
     * 	"och":	outgoing call history<br>
     * 	"mch":	missing call history<br>
     * 	"cch":	combination of ich och mch<br>
     * 	"spd":	speed dials entry ( only for "internal" )<br>
     * 	"fav":	favorites entry ( only for "internal" )<br>
     * <br>
     *
     * @param _location
     * @param _phonebook
     *
     * @throws BluezInvalidArgumentsException
     * @throws BluezFailedException
     */
    void Select(String _location, String _phonebook) throws BluezInvalidArgumentsException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return the entire phonebook object from the PSE server<br>
     * in plain string with vcard format, and store it in<br>
     * a local file.<br>
     * <br>
     * If an empty target file is given, a name will be<br>
     * automatically calculated for the temporary file.<br>
     * <br>
     * The returned path represents the newly created transfer,<br>
     * which should be used to find out if the content has been<br>
     * successfully transferred or if the operation fails.<br>
     * <br>
     * The properties of this transfer are also returned along<br>
     * with the object path, to avoid a call to GetProperties.<br>
     * <br>
     * Possible filters: Format, Order, Offset, MaxCount and<br>
     * Fields<br>
     * <br>
     *
     * @param _targetfile
     * @param _filters
     *
     * @throws BluezInvalidArgumentsException
     * @throws BluezForbiddenException
     */
    TwoTuple<Object,Map<?,?>> PullAll(String _targetfile, Map<?, ?> _filters) throws BluezInvalidArgumentsException, BluezForbiddenException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return an array of vcard-listing data where every entry<br>
     * consists of a pair of strings containing the vcard<br>
     * handle and the contact name. For example:<br>
     * 	"1.vcf" : "John"<br>
     * <br>
     * Possible filters: Order, Offset and MaxCount<br>
     * <br>
     *
     * @param _filters
     *
     * @throws BluezInvalidArgumentsException
     * @throws BluezForbiddenException
     */
    TwoTuple<String, String[]> List(Map<?, ?> _filters) throws BluezInvalidArgumentsException, BluezForbiddenException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Given a vcard handle, retrieve the vcard in the current<br>
     * phonebook object and store it in a local file.<br>
     * <br>
     * If an empty target file is given, a name will be<br>
     * automatically calculated for the temporary file.<br>
     * <br>
     * The returned path represents the newly created transfer,<br>
     * which should be used to find out if the content has been<br>
     * successfully transferred or if the operation fails.<br>
     * <br>
     * The properties of this transfer are also returned along<br>
     * with the object path, to avoid a call to GetProperties.<br>
     * <br>
     * Possbile filters: Format and Fields<br>
     * <br>
     *
     * @param _vcard
     * @param _targetfile
     * @param _filters
     *
     * @throws BluezInvalidArgumentsException
     * @throws BluezForbiddenException
     * @throws BluezFailedException
     */
    TwoTuple<Object,Map<?,?>> Pull(String _vcard, String _targetfile, Map<?, ?> _filters) throws BluezInvalidArgumentsException, BluezForbiddenException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Search for entries matching the given condition and<br>
     * return an array of vcard-listing data where every entry<br>
     * consists of a pair of strings containing the vcard<br>
     * handle and the contact name.<br>
     * <br>
     * vcard : name paired string match the search condition.<br>
     * <br>
     * field : the field in the vcard to search with<br>
     * 	{ "name" (default) | "number" | "sound" }<br>
     * value : the string value to search for<br>
     * <br>
     * <br>
     * Possible filters: Order, Offset and MaxCount<br>
     * <br>
     *
     * @param _field
     * @param _value
     * @param _filters
     *
     * @throws BluezInvalidArgumentsException
     * @throws BluezForbiddenException
     * @throws BluezFailedException
     */
    TwoTuple<String, String[]> Search(String _field, String _value, Map<?, ?> _filters) throws BluezInvalidArgumentsException, BluezForbiddenException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return the number of entries in the selected phonebook<br>
     * object that are actually used (i.e. indexes that<br>
     * correspond to non-NULL entries).<br>
     * <br>
     *
     * @throws BluezForbiddenException
     * @throws BluezFailedException
     */
    UInt16 GetSize() throws BluezForbiddenException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Attempt to update PrimaryCounter and SecondaryCounter.<br>
     * <br>
     *
     * @throws BluezNotSupportedException
     * @throws BluezForbiddenException
     * @throws BluezFailedException
     */
    void UpdateVersion() throws BluezNotSupportedException, BluezForbiddenException, BluezFailedException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Return All Available fields that can be used in Fields<br>
     * filter.<br>
     * <br>
     */
    String[] ListFilterFields();

}
