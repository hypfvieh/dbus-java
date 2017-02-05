package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusInterface;

public interface Adapter1 extends DBusInterface {

    void StartDiscovery() throws BluezNotReadyException, BluezFailedException;
    void StopDiscovery() throws BluezNotReadyException, BluezFailedException, BluezNotAuthorizedException;
    void RemoveDevice(Object _device) throws BluezFailedException, BluezInvalidArgumentException;

    void SetDiscoveryFilter(Map<?,?> _filter) throws BluezNotReadyException, BluezNotSupportedException, BluezFailedException;
}
