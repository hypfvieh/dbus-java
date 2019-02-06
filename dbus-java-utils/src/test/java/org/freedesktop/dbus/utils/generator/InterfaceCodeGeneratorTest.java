package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.util.FileIoUtil;

class InterfaceCodeGeneratorTest {

    @Test
    void testCreate() {
        String objectPath = "/org/fedoraproject/FirewallD1";
        String busName = "org.fedoraproject.FirewallD1";
        boolean ignoreDtd = true;

        Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);


        if (!StringUtils.isBlank(busName)) {
            String introspectionData = FileIoUtil.readFileToString("src/test/resources/CreateInterface/org.fedoraproject.FirewallD1.xml");

            InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
            try {
                Map<File, String> analyze = ci2.analyze(ignoreDtd);

                assertEquals(20, analyze.size());

                // writeToFile(SystemUtil.getTempDir() + File.separator + "CreateInterfaceTest", analyze);
            } catch (Exception _ex) {
                logger.error("Error while analyzing introspection data", _ex);
            }
        }
    }

}
