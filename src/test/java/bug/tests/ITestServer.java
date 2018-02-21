package bug.tests;

import org.freedesktop.dbus.DBusInterface;

public interface ITestServer extends DBusInterface {
    public byte identityByte(byte input);
    public byte[] identityByteArray(byte[] input);
}
