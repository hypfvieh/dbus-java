package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.utils.Util;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;
import java.util.Map;

class InterfaceCodeGeneratorTest {

    static InterfaceCodeGenerator loadDBusXmlFile(File _inputFile, String _objectPath, String _busName) {
        if (!StringUtils.isBlank(_busName)) {
            String introspectionData = Util.readFileToString(_inputFile);

            InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(false, introspectionData, _objectPath, _busName, null, false);
            return ci2;
        } else {
            fail("No valid busName given");
        }

        fail("Unable to load file: " + _inputFile);
        return null;
    }

    @Test
    void testCreateSelectedFirewallInterfaces() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
                new File("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml"), "/org/fedoraproject/FirewallD1", "org.fedoraproject.FirewallD1");
        Map<File, String> analyze = ci2.analyze(true);
        assertEquals(9, analyze.size());
    }

    @Test
    void testCreateAllFirewallInterfaces() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
                new File("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml"), "/org/fedoraproject/FirewallD1", "*");
        Map<File, String> analyze = ci2.analyze(true);
        assertEquals(20, analyze.size());
    }

    @Test
    void testCreateNetworkManagerWirelessInterface() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
                new File("src/test/resources/CreateInterface/networkmanager/org.freedesktop.NetworkManager.Device.Wireless.xml"),
                "/", "org.freedesktop.NetworkManager.Device.Wireless");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(1, analyze.size());

        String clzContent = analyze.get(analyze.keySet().iterator().next());

        assertTrue(clzContent.contains("@" + DBusInterfaceName.class.getSimpleName() + "(\"" + "org.freedesktop.NetworkManager.Device.Wireless" + "\")"));
        assertFalse(clzContent.contains("this._properties"));
        assertFalse(clzContent.contains("this._path"));
        assertFalse(clzContent.contains("this._interfaceName"));
    }

    @Test
    void testCreateSampleStructArgs() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
                new File("src/test/resources/CreateInterface/sample_struct_args.xml"), "/", "org.example");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(2, analyze.size()); // class with method and struct class expected

        String clzContent = analyze.get(new File("org", "ExampleMethodStruct.java"));

        assertTrue(clzContent.contains("@Position(0)"), "Position annotation expected");
        assertTrue(clzContent.contains("private final List<Integer> member0;"), "Final List<Integer> member expected");
        assertTrue(clzContent.contains("public ExampleMethodStruct(List<Integer> member0)"), "Constructor using List<Integer> expected");
        assertTrue(clzContent.contains("public List<Integer> getMember0()"), "Getter for Member of type List<Integer> expected");
    }
}
