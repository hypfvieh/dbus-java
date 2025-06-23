package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.utils.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class InterfaceCodeGeneratorTest {

    static InterfaceCodeGenerator loadDBusXmlFile(File _inputFile, String _objectPath, String _busName) {
        if (!Util.isBlank(_busName)) {
            String introspectionData = Util.readFileToString(_inputFile);

            return new InterfaceCodeGenerator(false, introspectionData, _objectPath, _busName, null, false, null);
        } else {
            fail("No valid busName given");
        }

        fail("Unable to load file: " + _inputFile);
        return null;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("createTestData")
    void testExtractData(String _description, File _input, String _dbusPath, String _filter, int _expected) throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
            _input, _dbusPath, _filter);
        Map<File, String> analyze = ci2.analyze(true);
        assertEquals(_expected, analyze.size());
    }

    static Stream<Arguments> createTestData() {
        return Stream.of(
            Arguments.of("FirewallD1: Test Extract All", new File("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml"),
                "/org/fedoraproject/FirewallD1", "*", 23),
            Arguments.of("FirewallD1: Test Extract Selected", new File("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml"),
                "/org/fedoraproject/FirewallD1", "org.fedoraproject.FirewallD1", 9),
            Arguments.of("DisplayConfig: Test Extract All", new File("src/test/resources/CreateInterface/mutter/org.gnome.Mutter.DisplayConfig.xml"),
                "/org/gnome/Mutter", "org.gnome.Mutter.DisplayConfig", 16)
        );
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

        String clzContent = analyze.get(new File("org", "ExampleMethodExampleArgStruct.java"));

        assertTrue(clzContent.contains("@Position(0)"), "Position annotation expected");
        assertTrue(clzContent.contains("private final List<Integer> member0;"), "Final List<Integer> member expected");
        assertTrue(clzContent.contains("public ExampleMethodExampleArgStruct(List<Integer> member0)"), "Constructor using List<Integer> expected");
        assertTrue(clzContent.contains("public List<Integer> getMember0()"), "Getter for Member of type List<Integer> expected");
    }

    @Test
    void testCreateStructNames() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
            new File("src/test/resources/CreateInterface/systemd/org.freedesktop.systemd1.Manager.xml"),
            "/org/freedesktop/systemd1", "org.freedesktop.systemd1.Manager");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(6, analyze.size());

        String managerFileContent = analyze.get(new File("org/freedesktop/systemd1/Manager.java"));
        assertTrue(managerFileContent.contains("void StartTransientUnit(List<StartTransientUnitPropertiesStruct> properties, List<StartTransientUnitAuxStruct> aux);"));

        String auxStructFileContent = analyze.get(new File("org/freedesktop/systemd1/StartTransientUnitAuxStruct.java"));
        assertTrue(auxStructFileContent.contains("private final String member0;"));
        assertTrue(auxStructFileContent.contains("private final List<StartTransientUnitAuxStructStruct> member1;"));

        String auxStructStructFileContent = analyze.get(new File("org/freedesktop/systemd1/StartTransientUnitAuxStructStruct.java"));
        assertTrue(auxStructStructFileContent.contains("private final String member0;"));
        assertTrue(auxStructStructFileContent.contains("private final Variant<?> member1;"));

        String propertiesStructFileContent = analyze.get(new File("org/freedesktop/systemd1/StartTransientUnitPropertiesStruct.java"));
        assertTrue(propertiesStructFileContent.contains("private final String member0;"));
        assertTrue(propertiesStructFileContent.contains("private final Variant<?> member1;"));

        /* For https://github.com/hypfvieh/dbus-java/issues/264  */
        String presetUnitFilesTupleContent = analyze.get(new File("org/freedesktop/systemd1/PresetUnitFilesTuple.java"));
        assertTrue(presetUnitFilesTupleContent
            .lines()
            .noneMatch(s -> s.contains("import PresetUnitFilesChangesStruct")),
            "Did not expect an import for a class of same package");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("createFindGenericNameData")
    void testFindGenericName(String _description, Set<String> _existingNames, String _expectedName) {
        assertEquals(_expectedName, InterfaceCodeGenerator.findNextGenericName(_existingNames));
    }

    static Stream<Arguments> createFindGenericNameData() {
        return Stream.of(
            Arguments.of("ABC -> D", Set.of("A", "B", "C"), "D"),
            Arguments.of("ABCD -> E", Set.of("A", "B", "C", "D"), "E"),
            Arguments.of("ABCDE -> F", Set.of("A", "B", "C", "D", "E"), "F"),
            Arguments.of("ABCDEF -> G", Set.of("A", "B", "C", "D", "E", "F"), "G")
        );
    }
}
