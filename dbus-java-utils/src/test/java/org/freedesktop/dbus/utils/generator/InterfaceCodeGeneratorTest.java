package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.util.FileIoUtil;

class InterfaceCodeGeneratorTest {

    @Test
    void testCreateFirewallInterfaces() {
        String objectPath = "/org/fedoraproject/FirewallD1";
        String busName = "org.fedoraproject.FirewallD1";
        boolean ignoreDtd = true;

        Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);


        if (!StringUtils.isBlank(busName)) {
            String introspectionData = FileIoUtil.readFileToString("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml");

            InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
            try {
                Map<File, String> analyze = ci2.analyze(ignoreDtd);

                assertEquals(20, analyze.size());

            } catch (Exception _ex) {
                logger.error("Error while analyzing introspection data", _ex);
            }
        }
    }

    @Test
    void testCreateNetworkManagerWirelessInterface() {
        String objectPath = "/";
        String busName = "org.freedesktop.NetworkManager.Device.Wireless";
        boolean ignoreDtd = true;

        Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);


        if (!StringUtils.isBlank(busName)) {
            String introspectionData = FileIoUtil.readFileToString("src/test/resources/CreateInterface/networkmanager/org.freedesktop.NetworkManager.Device.Wireless.xml");

            InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
            try {
                Map<File, String> analyze = ci2.analyze(ignoreDtd);

                assertEquals(1, analyze.size());
                
                String clzContent = analyze.get(analyze.keySet().iterator().next());
                
                assertTrue(clzContent.contains("@" + DBusInterfaceName.class.getSimpleName() + "(\"" + busName + "\")"));
                assertFalse(clzContent.contains("this._properties"));
                assertFalse(clzContent.contains("this._path"));
                assertFalse(clzContent.contains("this._interfaceName"));
            } catch (Exception _ex) {
                logger.error("Error while analyzing introspection data", _ex);
            }
        }
    }

}
