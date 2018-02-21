package bug.tests;


import java.util.List;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.CrossTestClient;

import com.github.hypfvieh.util.FileIoUtil;

public class TestClient {
    public static void main(String[] args) {
        try {
            /* init */
            DBusConnection conn = DBusConnection.getConnection(DBusBusType.SESSION);
            CrossTestClient instance = new CrossTestClient(conn);
            conn.exportObject("/Test", instance);
            
            List<String> testMessages = FileIoUtil.readFileToList("src/test/resources/sampletext.txt");
            
            ITestServer tests = conn.getRemoteObject("com.github.hypfvieh.bug.tests.TestServer", "/Test", ITestServer.class);
            for (String string : testMessages) {
                tests.identityByteArray(string.getBytes());
            }
            tests.identityByte((byte) '!');

            conn.disconnect();
        } catch (DBusException exDbe) {
            exDbe.printStackTrace();
            System.exit(1);
        }
    }
}
