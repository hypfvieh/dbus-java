package org.freedesktop.dbus.connections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class BusAddressTest {

    @Test
    void testUnescapesParameterValues() {
        BusAddress addr = BusAddress.of("unix:path=%2Ftmp%2Fdbus-test");
        assertEquals("/tmp/dbus-test", addr.getParameterValue("path"));
    }

    @Test
    void testEscapingRoundTrip() {
        BusAddress addr = BusAddress.of("tcp:host=my%20host,port=1234");
        assertEquals("my host", addr.getParameterValue("host"));

        // toString must re-escape the value so it can be parsed back into the same address
        String str = addr.toString();
        assertTrue(str.contains("host=my%20host"), "value must be re-escaped in toString: " + str);
        assertEquals("my host", BusAddress.of(str).getParameterValue("host"));
    }

    @Test
    void testOfParsesOnlyFirstAddressOfList() {
        BusAddress addr = BusAddress.of("unix:path=/first;tcp:host=example,port=1");
        assertTrue(addr.isBusType("unix"));
        assertEquals("/first", addr.getParameterValue("path")); // no ';tcp:...' leaking into the value
    }

    @Test
    void testParseAllReturnsEveryAddress() {
        List<BusAddress> all = BusAddress.parseAll("unix:path=/a;tcp:host=x,port=1");
        assertEquals(2, all.size());
        assertTrue(all.get(0).isBusType("unix"));
        assertEquals("/a", all.get(0).getParameterValue("path"));
        assertTrue(all.get(1).isBusType("tcp"));
        assertEquals("x", all.get(1).getParameterValue("host"));
        assertEquals("1", all.get(1).getParameterValue("port"));
    }
}
