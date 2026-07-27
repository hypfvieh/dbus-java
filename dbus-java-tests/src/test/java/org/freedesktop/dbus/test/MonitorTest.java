package org.freedesktop.dbus.test;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.matchrules.DBusMatchRuleBuilder;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.constants.MessageTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that a connection turned into a monitor via {@code becomeMonitor(...)} receives copies of
 * the traffic flowing over the (embedded) bus.
 */
public class MonitorTest extends AbstractDBusBaseTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testMonitorReceivesBusTraffic() throws Exception {
        BlockingQueue<Message> received = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);

        try (DBusConnection monitorConn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
            // empty rule list -> monitor all messages
            monitorConn.becomeMonitor(List.of(), msg -> {
                received.add(msg);
                if ("PingSignal".equals(msg.getName())) {
                    latch.countDown();
                }
            });

            // generate traffic from another connection; the monitor must see a copy
            serverconn.sendMessage(new MonitorTestSignals.PingSignal(getTestObjectPath(), "hello-monitor"));

            assertTrue(latch.await(15, TimeUnit.SECONDS), "monitor did not receive the emitted signal");

            Message sig = received.stream()
                .filter(m -> "PingSignal".equals(m.getName()))
                .findFirst()
                .orElseThrow();

            assertEquals("org.freedesktop.dbus.test.MonitorTestSignals", sig.getInterface());
            assertEquals(getTestObjectPath(), sig.getPath());
            assertEquals("hello-monitor", sig.getParameters()[0]);
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testMonitorFiltersByMatchRule() throws Exception {
        BlockingQueue<Message> received = new LinkedBlockingQueue<>();
        CountDownLatch pingLatch = new CountDownLatch(1);

        // only messages matching this rule (the PingSignal member) must reach the monitor
        DBusMatchRule rule = DBusMatchRuleBuilder.create()
            .withType(MessageTypes.SIGNAL)
            .withInterface("org.freedesktop.dbus.test.MonitorTestSignals")
            .withMember("PingSignal")
            .build();

        try (DBusConnection monitorConn = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
            monitorConn.becomeMonitor(List.of(rule), msg -> {
                received.add(msg);
                if ("PingSignal".equals(msg.getName())) {
                    pingLatch.countDown();
                }
            });

            // send the non-matching signal FIRST, the matching one SECOND: once the (later) PingSignal has
            // been observed, a matching PongSignal would already have been delivered too - so its absence is
            // a reliable proof of filtering without relying on a fixed sleep
            serverconn.sendMessage(new MonitorTestSignals.PongSignal(getTestObjectPath(), "should-be-filtered"));
            serverconn.sendMessage(new MonitorTestSignals.PingSignal(getTestObjectPath(), "hello-monitor"));

            assertTrue(pingLatch.await(15, TimeUnit.SECONDS), "monitor did not receive the matching signal");
            assertTrue(received.stream().noneMatch(m -> "PongSignal".equals(m.getName())),
                "monitor must not receive signals that do not match its match rule");
        }
    }

    @DBusInterfaceName("org.freedesktop.dbus.test.MonitorTestSignals")
    public interface MonitorTestSignals extends DBusInterface {
        class PingSignal extends DBusSignal {
            public PingSignal(String _path, String _value) throws DBusException {
                super(_path, _value);
            }
        }

        class PongSignal extends DBusSignal {
            public PongSignal(String _path, String _value) throws DBusException {
                super(_path, _value);
            }
        }
    }
}
