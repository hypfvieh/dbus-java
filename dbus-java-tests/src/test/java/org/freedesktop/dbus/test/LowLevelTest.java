package org.freedesktop.dbus.test;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MessageFactory;
import org.freedesktop.dbus.utils.AddressBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;

public class LowLevelTest extends AbstractDBusDaemonBaseTest {

    @Test
    public void testLowLevel() throws ParseException, IOException, DBusException, InterruptedException {
        BusAddress address = BusAddress.of(AddressBuilder.getSessionConnection(null));
        logger.debug("Testing using address: {}", address);

        try (AbstractTransport conn = TransportBuilder.create(address).build()) {
            waitIfTcp();
            MessageFactory messageFactory = conn.getTransportConnection().getMessageFactory();
            Message m = messageFactory.createMethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m.getClass() + "");
            logger.debug(m + "");
            m = conn.readMessage();
            logger.debug(m.getClass() + "");
            logger.debug(m + "");
            m = messageFactory.createMethodCall("org.freedesktop.DBus", "/", null, "Hello", (byte) 0, null);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m + "");

            m = messageFactory.createMethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", (byte) 0, "su", "org.testname", 0);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m + "");
            m = messageFactory.createSignal(null, "/foo", "org.foo", "Foo", null);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m + "");
        }
    }

}
