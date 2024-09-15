package sample.issue;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.test.AbstractDBusDaemonBaseTest;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Issue268Test extends AbstractDBusDaemonBaseTest {

    private static final String BUS_NAME = Issue268Test.class.getName();
    private static final String OBJECT_PATH = "/" + Issue268Test.class.getSimpleName().toLowerCase();

    private final CountDownLatch waitLatch = new CountDownLatch(1);

    @Test
    void testSignal() throws Exception {
        MethodCall.setDefaultTimeout(1000000);
        List<DBusSignal> failed = new ArrayList<>();
        try (DBusConnection exportDBusConn = export(failed);
            DBusConnection receiveDBusConn = receive()) {

           exportDBusConn.sendMessage(new Issue268Signal.MessageReceivedV2(OBJECT_PATH,
                   System.currentTimeMillis(),
                   "sender",
                   new byte[]{},
                   "message",
                   Map.of()));

           waitLatch.await(10, TimeUnit.SECONDS);
           assertTrue(failed.isEmpty(), "Expected all signals to be handled properly");
       }
    }

    DBusConnection export(List<DBusSignal> _failed) throws Exception {
        DBusConnection dBusConn = DBusConnectionBuilder.forType(DBusConnection.DBusBusType.SESSION)
            .withUnknownSignalHandler(s -> {
                _failed.add(s);
                logger.debug("Signal handling failed");
                waitLatch.countDown();
            })
            .build();

        dBusConn.requestBusName(BUS_NAME);
        dBusConn.exportObject(() -> OBJECT_PATH);

        return dBusConn;
    }

    DBusConnection receive() throws Exception {
        DBusConnection dBusConn = DBusConnectionBuilder.forType(DBusConnection.DBusBusType.SESSION)
            .build();
        Issue268Signal signal = dBusConn.getRemoteObject(BUS_NAME, OBJECT_PATH, Issue268Signal.class);

        DBusSigHandler<Issue268Signal.MessageReceivedV2> dbusMsgHandler = messageReceived -> {
            logger.debug("Received proper signal");
            waitLatch.countDown();
        };

        dBusConn.addSigHandler(Issue268Signal.MessageReceivedV2.class, signal, dbusMsgHandler);
        return dBusConn;
    }

    public interface Issue268Signal extends DBusInterface {

        class MessageReceivedV2 extends DBusSignal {

            private final long timestamp;
            private final String sender;
            private final byte[] groupId;
            private final String message;
            private final Map<String, Variant<?>> extras;

            public MessageReceivedV2(
                    String _objectpath, long _timestamp,
                    String _sender,  byte[] _groupId,
                    String _message, Map<String, Variant<?>> _extras
            ) throws DBusException {
                super(_objectpath, _timestamp, _sender, _groupId, _message, _extras);
                this.timestamp = _timestamp;
                this.sender = _sender;
                this.groupId = _groupId;
                this.message = _message;
                this.extras = _extras;
            }

            public long getTimestamp() {
                return timestamp;
            }

            public String getSender() {
                return sender;
            }

            public byte[] getGroupId() {
                return groupId;
            }

            public String getMessage() {
                return message;
            }

            public Map<String, Variant<?>> getExtras() {
                return extras;
            }
        }
    }
}
