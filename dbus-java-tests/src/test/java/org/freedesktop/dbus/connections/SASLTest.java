package org.freedesktop.dbus.connections;

import org.freedesktop.dbus.connections.SASL.Command;
import org.freedesktop.dbus.connections.SASL.SaslCommand;
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

}
