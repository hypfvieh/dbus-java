package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;

public interface GattDescriptor1 extends DBusInterface {

    byte[] ReadValue(Map<String, Variant<?>> _flags) throws BluezFailedException, BluezInProgressException,
                                                        BluezNotPermittedException, BluezNotAuthorizedException,
                                                        BluezNotSupportedException;
    void WriteValue(byte[] _value, Map<String, Variant<?>> _flags) throws BluezFailedException, BluezInProgressException,
                                                        BluezNotPermittedException, BluezNotAuthorizedException,
                                                        BluezNotSupportedException;;
}
