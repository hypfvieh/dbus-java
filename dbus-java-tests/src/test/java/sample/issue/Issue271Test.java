package sample.issue;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.AbstractDBusDaemonBaseTest;
import org.freedesktop.dbus.types.UInt16;
import org.junit.jupiter.api.Test;

public class Issue271Test extends AbstractDBusDaemonBaseTest {

    @Test
    void testExportTuple() {
        assertDoesNotThrow(() -> {
            try (DBusConnection connection = DBusConnectionBuilder.forSessionBus().build()) {
                Issue271Mock mock = new Issue271Mock();
                connection.requestBusName(Issue271Test.class.getPackageName());
                connection.exportObject(mock.getObjectPath(), mock);
            }
        });
    }

    public static class Issue271Tuple extends Tuple {
        @Position(0)
        private byte   info;
        @Position(1)
        private UInt16 field;

        public Issue271Tuple(byte _info, UInt16 _field) {
            info = _info;
            field = _field;
        }

        public byte getInfo() {
            return info;
        }

        public void setInfo(byte _info) {
            info = _info;
        }

        public UInt16 getField() {
            return field;
        }

        public void setField(UInt16 _field) {
            field = _field;
        }

    }

    public static class Issue271Mock implements Issue271Interface {

        @Override
        public String getObjectPath() {
            return "/issue271";
        }

        @Override
        public Issue271Tuple GetDetails() {
            return new Issue271Tuple((byte) 0, new UInt16(11));
        }

    }

    public interface Issue271Interface extends DBusInterface {

        @SuppressWarnings("checkstyle:MethodName")
        Issue271Tuple GetDetails();

    }
}
