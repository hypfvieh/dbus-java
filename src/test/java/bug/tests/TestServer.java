package bug.tests;

import org.freedesktop.DBus;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;



public class TestServer implements ITestServer {
    
    //private DBusConnection conn;
    private boolean run = true;

    public TestServer() {
       // this.conn = _conn;
    }
    
    public boolean isRun() {
        return run;
    }

    public void setRun(boolean _run) {
        run = _run;
    }
    
    @DBus.Description("Returns whatever it is passed")
    public byte identityByte(byte input) {
        System.out.println("Received byte: " + input);
        return input;
    }
    
    
    @DBus.Description("Returns whatever it is passed")
    public byte[] identityByteArray(byte[] input) {
        System.out.println("Received byte array: " + input);
        System.out.println("String representation of byte array: " + new String(input));
        return input;
    }
    
    public static void main(String[] args) {
        try {
            DBusConnection conn = DBusConnection.getConnection(DBusBusType.SESSION);
            conn.requestBusName("com.github.hypfvieh.bug.tests.TestServer");
            TestServer cts = new TestServer();
            conn.exportObject("/Test", cts);
            synchronized (cts) {
                while (cts.run) {
                    try {
                        cts.wait();
                    } catch (InterruptedException exIe) {
                    }
                }
            }
            conn.disconnect();
            System.exit(0);
        } catch (DBusException exDe) {
            exDe.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }
}
