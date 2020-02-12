package org.bluez;

import java.util.Map;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2020-02-12.<br>
 * Based on bluez Documentation: thermometer-api.txt.<br>
 * <br>
 * <b>Service:</b> unique name<br>
 * <b>Interface:</b> org.bluez.ThermometerWatcher1<br>
 * <br>
 * <b>Object path:</b><br>
 *             freely definable<br>
 * <br>
 */
public interface ThermometerWatcher1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This callback gets called when a measurement has been<br>
     * scanned in the thermometer.<br>
     * <br>
     * Measurement:<br>
     * <br>
     * 	int16 Exponent:<br>
     * 	int32 Mantissa:<br>
     * <br>
     * 		Exponent and Mantissa values as<br>
     * 		extracted from float value defined by<br>
     * 		IEEE-11073-20601.<br>
     * <br>
     * 		Measurement value is calculated as<br>
     * 		(Mantissa) * (10^Exponent)<br>
     * <br>
     * 		For special cases Exponent is<br>
     * 		set to 0 and Mantissa is set to<br>
     * 		one of following values:<br>
     * <br>
     * 		+(2^23 - 1)	NaN (invalid or<br>
     * 	missing data)<br>
     * 		-(2^23)		NRes<br>
     * 		+(2^23 - 2)	+Infinity<br>
     * 		-(2^23 - 2)	-Infinity<br>
     * <br>
     * 	string Unit:<br>
     * <br>
     * 		Possible values: "celsius" or<br>
     * 	"fahrenheit"<br>
     * <br>
     * 	uint64 Time (optional):<br>
     * <br>
     * 		Time of measurement, if<br>
     * 		supported by device.<br>
     * 		Expressed in seconds since epoch.<br>
     * <br>
     * 	string Type (optional):<br>
     * <br>
     * 		Only present if measurement type<br>
     * 		is known.<br>
     * <br>
     * 		Possible values: "armpit", "body",<br>
     * "ear", "finger", "intestines",<br>
     * "mouth", "rectum", "toe",<br>
     * "tympanum"<br>
     * <br>
     * 	string Measurement:<br>
     * <br>
     * 		Possible values: "final" or<br>
     * 	"intermediate"<br>
     * 
     * @param _measurement
     */
    void MeasurementReceived(Map<String, Variant<?>> _measurement);

}
