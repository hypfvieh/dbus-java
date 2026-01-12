package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.config.DBusSysProps;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.matchrules.DBusMatchRule;
import org.freedesktop.dbus.matchrules.DBusMatchRuleBuilder;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.freedesktop.dbus.test.helper.signals.SampleSignals;
import org.freedesktop.dbus.test.helper.signals.SampleSignals.TestSignal;
import org.freedesktop.dbus.types.UInt32;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 *
 */
class EmbeddedDBusDaemonTest extends AbstractBaseTest {

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testAddMatchRule() throws DBusException, InterruptedException {

        doWithEmbeddedDaemon((daemon, addr) -> {
            try {
                CountDownLatch countDown = new CountDownLatch(1);
                AtomicInteger counter = new AtomicInteger();
                try (DBusConnection handlerConn = DBusConnectionBuilder.forAddress(addr)
                        .withShared(false).build();
                    DBusConnection senderConn = DBusConnectionBuilder.forAddress(addr)
                        .withShared(false).build()) {

                    DBusMatchRule rule = DBusMatchRuleBuilder.create()
                        .withInterface(SampleSignals.class.getName())
                        .withSender(senderConn.getUniqueName())
                        .build();

                    handlerConn.addSigHandler(rule, s -> {
                        logger.info(">>> Got signal: {}", s);
                        counter.incrementAndGet();
                        countDown.countDown();
                    });

                    senderConn.sendMessage(new TestSignal("/some/rule/Test", "XXX", new UInt32(21)));
                    countDown.await(5, TimeUnit.SECONDS);
                    assertEquals(1, counter.get(), "Expected signal to be handled");

                }
            } catch (Exception _ex) {
                fail(_ex);
            }
        });

    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testStartAndConnectEmbeddedDBusDaemon() throws DBusException {
        doWithEmbeddedDaemon((daemon, addr) -> {
            try (DBusConnection conn = DBusConnectionBuilder.forAddress(addr).build()) {
                logger.debug("Connected to embedded DBus {}", addr);
            } catch (Exception _ex) {
                fail("Connection to EmbeddedDbusDaemon failed", _ex);
                logger.error("Error connecting to EmbeddedDbusDaemon", _ex);
            }
        });
    }

    private void doWithEmbeddedDaemon(BiConsumer<EmbeddedDBusDaemon, BusAddress> _handler) {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);

        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        logger.debug("Starting embedded bus on address {})", listenBusAddress);
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            logger.debug("Started embedded bus on address {}", listenBusAddress);
            daemon.startInBackgroundAndWait(MAX_WAIT);

            if (_handler != null) {
                _handler.accept(daemon, busAddress);
            }

        } catch (IOException _ex) {
            fail("Failed to start EmbeddedDbusDaemon", _ex);
            logger.error("Error starting EmbeddedDbusDaemon", _ex);
        }

    }

    @Test
    void testStartStop() throws Exception {

        for (int i = 0; i < 2; i++) {
            String address = TransportBuilder.createDynamicSession(TransportBuilder.getRegisteredBusTypes().getFirst(), true);
            BusAddress busAddress = BusAddress.of(address);

            // initialize
            EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(busAddress);

            if (busAddress.isBusType("TCP")) {
                String addrStr  = busAddress.removeParameter("listen").toString();
                System.setProperty(DBusSysProps.DBUS_SESSION_BUS_ADDRESS, addrStr);
            }

            // start the daemon in background to not block the test
            AtomicReference<Exception> exception = new AtomicReference<>();
            Thread daemonThread = new Thread(() -> {
                try {
                    daemon.startInForeground();
                } catch (Exception _ex) {
                    exception.set(_ex);
                    _ex.printStackTrace();
                }
            });
            daemonThread.start();

            // give the daemon time to start
            Thread.sleep(1000);
            daemon.close();
            assertEquals(null, exception.get()); // assertEquals() gives a better error message
        }

        Entry<Thread, StackTraceElement[]> elems = null;
        for (Entry<Thread, StackTraceElement[]> stacks : Thread.getAllStackTraces().entrySet()) {
            if (stacks.getKey().getName().startsWith(DBusDaemon.class.getSimpleName())) {
                elems = stacks;
                break;
            }
        }

        if (elems != null) {
            System.out.println("Found possibly running instances: " + elems.getKey().getName());
            for (StackTraceElement st : elems.getValue()) {
                System.out.println("\t" + st.toString());
            }
            fail("All dbus daemon threads should have been terminated");
        }
    }
}
