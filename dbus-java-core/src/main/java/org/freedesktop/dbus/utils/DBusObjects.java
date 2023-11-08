package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidBusNameException;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Various validations.
 *
 * @since 5.0.0 - 2023-11-08
 * @author hypfvieh
 */
public final class DBusObjects {
    private static final int     MAX_NAME_LENGTH      = 255;
    private static final Pattern OBJECT_REGEX_PATTERN = Pattern.compile("^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$");
    private static final Pattern BUSNAME_REGEX        = Pattern.compile("^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$");
    private static final Pattern CONNID_REGEX         = Pattern.compile("^:[0-9]*\\.[0-9]*$");

    private DBusObjects() {
    }

    private static <X extends DBusException> String assertBase(String _str, Predicate<String> _validation, Function<String, X> _exSupplier, String _customMessage) throws X {
        if (_str == null) {
            _exSupplier.apply(_customMessage != null ? _customMessage : null);
        } else if (_str.isBlank()) {
            _exSupplier.apply(_customMessage != null ? _customMessage : "<Empty String>");
        } else if (!_validation.test(_str)) {
            _exSupplier.apply(_customMessage != null ? _customMessage : _str);
        }

        return _str;
    }

    public static String requireObjectPath(String _objectPath) throws InvalidObjectPathException {
        return assertBase(_objectPath, DBusObjects::validateObjectPath, x -> new InvalidObjectPathException(x), null);
    }

    public static String requireBusName(String _busName) throws InvalidBusNameException {
        return requireBusName(_busName, null);
    }

    public static String requireBusName(String _busName, String _customMessage) throws InvalidBusNameException {
        return assertBase(_busName, DBusObjects::validateNotBusName, x -> new InvalidBusNameException(x), _customMessage);
    }

    public static String requireNotBusName(String _busName, String _customMessage) throws InvalidBusNameException {
        return assertBase(_busName, DBusObjects::validateBusName, x -> new InvalidBusNameException(x), _customMessage);
    }

    public static String requireConnectionId(String _connId) throws InvalidBusNameException {
        return assertBase(_connId, DBusObjects::validateConnectionId, x -> new InvalidBusNameException(x), null);
    }

    public static String requireBusNameOrConnectionId(String _busNameOrConnId) throws InvalidBusNameException {
        requireBusName(_busNameOrConnId);
        requireConnectionId(_busNameOrConnId);
        return _busNameOrConnId;
    }

    public static boolean validateBusName(String _busName) {
        return (_busName != null) && (_busName.length() < MAX_NAME_LENGTH) && BUSNAME_REGEX.matcher(_busName).matches();
    }

    public static boolean validateNotBusName(String _busName) {
        return !validateBusName(_busName);
    }

    public static boolean validateObjectPath(String _objectPath) {
        return !validateNotObjectPath(_objectPath);
    }

    public static boolean validateNotObjectPath(String _objectPath) {
        return ((_objectPath != null) && (_objectPath.length() > MAX_NAME_LENGTH)) || !_objectPath.startsWith("/") || !OBJECT_REGEX_PATTERN.matcher(_objectPath).matches();
    }

    public static boolean validateNotConnectionId(String _connectionId) {
        return !validateConnectionId(_connectionId);
    }

    public static boolean validateConnectionId(String _connectionId) {
        return (_connectionId != null) && CONNID_REGEX.matcher(_connectionId).matches();
    }

}
