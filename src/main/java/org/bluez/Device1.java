package org.bluez;

import org.bluez.exceptions.BluezAlreadyConnectedException;
import org.bluez.exceptions.BluezAuthenticationFailedException;
import org.bluez.exceptions.BluezConnectFailedException;
import org.bluez.exceptions.BluezDoesNotExistsException;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInProgressException;
import org.bluez.exceptions.BluezInvalidArgumentException;
import org.bluez.exceptions.BluezNotConnectedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusInterface;

public interface Device1 extends DBusInterface {
    void Disconnect() throws BluezNotConnectedException;
    void Connect() throws BluezNotReadyException, BluezFailedException, BluezAlreadyConnectedException, BluezInProgressException;
    void ConnectProfile(String UUID) throws BluezDoesNotExistsException, BluezAlreadyConnectedException, BluezConnectFailedException;
    void DisconnectProfile(String UUID) throws BluezDoesNotExistsException, BluezFailedException, BluezNotConnectedException, BluezNotSupportedException;
    void CancelPairing() throws BluezDoesNotExistsException, BluezFailedException;
    void Pair() throws BluezInvalidArgumentException, BluezFailedException, BluezAuthenticationFailedException;
}
