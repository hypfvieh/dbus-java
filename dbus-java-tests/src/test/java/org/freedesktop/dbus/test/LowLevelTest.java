package org.freedesktop.dbus.test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.utils.AddressBuilder;
import org.freedesktop.dbus.utils.Util;
import org.junit.jupiter.api.Test;

public class LowLevelTest extends AbstractDBusBaseTest {

    @Test
    public void testLowLevel() throws ParseException, IOException, DBusException, InterruptedException {
        BusAddress address = new BusAddress(getAddress());
        logger.debug("Testing using address: {}", address);

        try (AbstractTransport conn = TransportBuilder.create(address).build()) {
            Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m.getClass() + "");
            logger.debug(m + "");
            m = conn.readMessage();
            logger.debug(m.getClass() + "");
            logger.debug(m + "");
            m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", (byte) 0, null);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m + "");

            m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", (byte) 0, "su", "org.testname", 0);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m + "");
            m = new DBusSignal(null, "/foo", "org.foo", "Foo", null);
            conn.writeMessage(m);
            waitIfTcp();
            m = conn.readMessage();
            logger.debug(m + "");
        }
    }

    static String getAddress() throws DBusException {
        if (!TransportBuilder.getRegisteredBusTypes().contains("UNIX")) {
            return System.getProperty(AbstractConnection.TCP_ADDRESS_PROPERTY);
        }

        String s = System.getenv("DBUS_SESSION_BUS_ADDRESS");
        if (s == null) {
            // address gets stashed in $HOME/.dbus/session-bus/`dbus-uuidgen --get`-`sed 's/:\(.\)\..*/\1/' <<<
            // $DISPLAY`
            String display = System.getenv("DISPLAY");
            if (null == display) {
                throw new RuntimeException("Cannot Resolve Session Bus Address");
            }
            if (!display.startsWith(":") && display.contains(":")) { // display seems to be a remote display
                                                                     // (e.g. X forward through SSH)
                display = display.substring(display.indexOf(':'));
            }

            String uuid = AddressBuilder.getDbusMachineId(null);
            String homedir = System.getProperty("user.home");
            File addressfile = new File(homedir + "/.dbus/session-bus",
                    uuid + "-" + display.replaceAll(":([0-9]*)\\..*", "$1"));
            if (!addressfile.exists()) {
                throw new RuntimeException("Cannot Resolve Session Bus Address");
            }
            Properties readProperties = Util.readProperties(addressfile);
            String sessionAddress = readProperties.getProperty("DBUS_SESSION_BUS_ADDRESS");
            if (Util.isEmpty(sessionAddress)) {
                throw new RuntimeException("Cannot Resolve Session Bus Address");
            }
            return sessionAddress;
        }

        return s;
    }
}
