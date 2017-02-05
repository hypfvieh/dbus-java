package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotPermittedException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;

public interface GattCharacteristic1 extends DBusInterface {
    @SuppressWarnings("rawtypes")
    byte[] ReadValue(Map<String,Variant> _options)
               throws BluezFailedException, BluezInProgressException,
                   BluezNotPermittedException, BluezNotAuthorizedException, BluezNotSupportedException;

    @SuppressWarnings("rawtypes")
    void WriteValue(byte[] _value, Map<String,Variant> _options)
            throws BluezFailedException, BluezInProgressException, BluezNotPermittedException,
                BluezNotAuthorizedException, BluezNotSupportedException;

    void StartNotify() throws BluezFailedException, BluezInProgressException, BluezNotSupportedException;

    void StopNotify() throws BluezFailedException;
}
