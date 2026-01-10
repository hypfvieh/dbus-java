package sample.issue;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.test.AbstractDBusDaemonBaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Issue290Test extends AbstractDBusDaemonBaseTest {

    private static DBusConnection connection;

    @BeforeAll
    static void beforeAll() throws DBusException {
        connection = DBusConnectionBuilder.forSessionBus().build();
        Issue290Impl mock = new Issue290Impl();
        connection.requestBusName(Issue290Impl.class.getPackageName());
        connection.exportObject(mock.getObjectPath(), mock);
    }

    @AfterAll
    static void afterAll() {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testGetRemoteObjectWithDBusPath() {
        assertDoesNotThrow(() -> connection.getRemoteObject(
            Issue290Impl.class.getPackageName(),
            new DBusPath("/"),
            ObjectManager.class));

    }

    @Test
    void testGetRemoteObjectWithString() {
        assertDoesNotThrow(() -> connection.getRemoteObject(
            Issue290Impl.class.getPackageName(),
            "/",
            ObjectManager.class));
    }

    public interface Issue290Interface extends DBusInterface {
        String something();
    }

    public static class Issue290Impl implements Issue290Interface {

        @Override
        public String getObjectPath() {
            return "/issue290";
        }

        @Override
        public String something() {
            return "some result";
        }

    }
}
