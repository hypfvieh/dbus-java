package org.freedesktop.dbus.connections;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.SASL.Command;
import org.freedesktop.dbus.connections.SASL.SaslCommand;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.connections.transports.TransportBuilder.SaslAuthMode;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.test.AbstractBaseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SASLTest extends AbstractBaseTest {

    @Test
    public void testCommandNoData() throws IOException {
        Command cmdData = new Command("DATA ");
        assertEquals(SaslCommand.DATA, cmdData.getCommand());
        assertNull(cmdData.getData());
    }

    @Test
    public void testCommandWithData() throws IOException {
        Command cmdData = new Command("DATA blafasel");
        assertEquals(SaslCommand.DATA, cmdData.getCommand());
        assertEquals("blafasel", cmdData.getData());
    }

    @Test
    public void testCommandAuth() throws IOException {
        Command cmdData = new Command("AUTH ");
        assertEquals(SaslCommand.AUTH, cmdData.getCommand());
        assertNull(cmdData.getData());
    }

    @Test
    public void testAnonymousAuthentication() throws DBusException {
        String protocolType = TransportBuilder.getRegisteredBusTypes().getFirst();
        String newAddress = TransportBuilder.createDynamicSession(protocolType, false);

        BusAddress busAddress = BusAddress.of(newAddress);
        BusAddress listenBusAddress = BusAddress.of(newAddress + ",listen=true");

        logger.debug("Starting embedded bus on address {})", listenBusAddress);
        try (EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon(listenBusAddress)) {
            daemon.setSaslAuthMode(SaslAuthMode.AUTH_ANONYMOUS);
            daemon.startInBackgroundAndWait(MAX_WAIT);
            logger.debug("Started embedded bus on address {}", listenBusAddress);

            // connect to started daemon process
            logger.info("Connecting to embedded DBus {}", busAddress);

            try (DBusConnection conn = DBusConnectionBuilder.forAddress(busAddress)
                .transportConfig().configureSasl().withAuthMode(SaslAuthMode.AUTH_ANONYMOUS).back().back()
                .build()) {

                logger.debug("Connected to embedded DBus {}", busAddress);

                assertEquals(SaslAuthMode.AUTH_ANONYMOUS.getAuthMode(), conn.getTransportConfig().getSaslConfig().getAuthMode());
            } catch (Exception _ex) {
                fail("Connection to EmbeddedDbusDaemon failed", _ex);
                logger.error("Error connecting to EmbeddedDbusDaemon", _ex);
            }
        } catch (IOException _ex1) {
            fail("Failed to start EmbeddedDbusDaemon", _ex1);
            logger.error("Error starting EmbeddedDbusDaemon", _ex1);
        }
    }
}
