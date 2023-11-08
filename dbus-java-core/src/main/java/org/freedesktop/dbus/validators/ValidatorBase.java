package org.freedesktop.dbus.validators;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.InvalidBusNameException;
import org.freedesktop.dbus.exceptions.InvalidObjectPathException;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Various validators which may throw proper exception when validation failed.
 * 
 * @since 5.0.0 - 2023-11-08
 * @author hypfvieh
 */
public class ValidatorBase {
    private static final int     MAX_NAME_LENGTH      = 255;
    private static final Pattern OBJECT_REGEX_PATTERN = Pattern.compile("^/([-_a-zA-Z0-9]+(/[-_a-zA-Z0-9]+)*)?$");
    private static final Pattern BUSNAME_REGEX        = Pattern.compile("^[-_a-zA-Z][-_a-zA-Z0-9]*(\\.[-_a-zA-Z][-_a-zA-Z0-9]*)*$");
    private static final Pattern CONNID_REGEX         = Pattern.compile("^:[0-9]*\\.[0-9]*$");

//    private final Class<R>       returnType;

    private final String inputStr;
    
    private ValidatorBase(String _input) {
        inputStr = _input;
    }
    
    public static ValidatorBase of(String _str) {
        return new ValidatorBase(_str);
    }
    
    private <X extends DBusException> void assertBase(Function<String, X> _exSupplier) throws X {
        if (inputStr == null) {
            throw _exSupplier.apply(null);
        } else if (inputStr.isBlank()) {
            throw _exSupplier.apply("<Empty String>");
        }
    }
    
    public String assertObjectPath() throws InvalidObjectPathException {
        assertBase(x -> new InvalidObjectPathException(x));
        
        if (inputStr.length() > MAX_NAME_LENGTH
            || !inputStr.startsWith("/")
            || !OBJECT_REGEX_PATTERN.matcher(inputStr).matches()) {
            throw new InvalidObjectPathException(inputStr);
        }
        
        return inputStr;
    }
    
    public String assertBusName() throws InvalidBusNameException {
        assertBase(x -> new InvalidBusNameException(x));
        if (inputStr.length() > MAX_NAME_LENGTH || !BUSNAME_REGEX.matcher(inputStr).matches()) {
            throw new InvalidBusNameException(inputStr);
        }
        return inputStr;
    }
    
    public String assertConnectionId() throws InvalidBusNameException {
        assertBase(x -> new InvalidBusNameException(x));
        if (!CONNID_REGEX.matcher(inputStr).matches()) {
            new InvalidBusNameException(inputStr);
        }
        return inputStr;
    }
    
    public ValidatorBase objectPath() {
        return this;
    }
    
}
