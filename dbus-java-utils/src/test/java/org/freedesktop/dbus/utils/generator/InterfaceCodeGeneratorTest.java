package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.*;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.utils.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class InterfaceCodeGeneratorTest {

    static InterfaceCodeGenerator loadDBusXmlFile(File _inputFile, String _objectPath, String _busName) {
        return loadDBusXmlFile(false, _inputFile, _objectPath, _busName);
    }

    static InterfaceCodeGenerator loadDBusXmlFile(boolean _createPropertyMethods, File _inputFile, String _objectPath, String _busName) {
        if (!Util.isBlank(_busName)) {
            String introspectionData = Util.readFileToString(_inputFile);

            return new InterfaceCodeGenerator(false, introspectionData, _objectPath, _busName, null, _createPropertyMethods, null, false);
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
    void testHandleKebabCase() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
                new File("src/test/resources/CreateInterface/xdg-desktop/org.freedesktop.portal.PowerProfileMonitor.xml"),
                "/", "org.freedesktop.portal.PowerProfileMonitor");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(1, analyze.size());

        String clzContent = analyze.get(analyze.keySet().iterator().next());

        assertLineEquals(12, clzContent, "    @DBusBoundProperty(name = \"power-saver-enabled\")");
        assertLineEquals(13, clzContent, "    boolean isPowerSaverEnabled();");

        assertLineEquals(15, clzContent, "    @DBusBoundProperty(name = \"version\")");
        assertLineEquals(16, clzContent, "    UInt32 getVersion();");
    }

    @Test
    void testHandleReservedMethodNames() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
                new File("src/test/resources/CreateInterface/xdg-desktop/org.freedesktop.portal.Clipboard.xml"),
                "/", "org.freedesktop.portal.Clipboard");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(1, analyze.size());

        String clzContent = analyze.get(analyze.keySet().iterator().next());

        assertLineEquals(73, clzContent, "        public UInt32 getSerialFromBus() {");
    }

    @Test
    void testHandleReservedMethodNames2() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
                new File("src/test/resources/CreateInterface/sample_reserved_names.xml"),
                "/", "org.example.Reserved");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(1, analyze.size());

        String clzContent = analyze.get(analyze.keySet().iterator().next());

        assertLineEquals(12, clzContent, "    @DBusMemberName(\"getWireData\")");
        assertLineEquals(13, clzContent, "    int getWireDataFromBus();");

        assertLineNotEquals(23, clzContent, "        @DBusMemberName(\"serial\")");
        assertLineEquals(24, clzContent, "        public DBusPath getSerialFromBus() {");
    }

    @Test
    void testHandleStructSignals() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
            new File("src/test/resources/CreateInterface/xdg-desktop/org.freedesktop.portal.GlobalShortcuts.xml"),
            "/", "org.freedesktop.portal.GlobalShortcuts");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(3, analyze.size());

        String primaryFile = analyze.entrySet().stream()
            .filter(e -> e.getKey().getName().equals("GlobalShortcuts.java"))
            .findFirst()
            .orElseThrow()
            .getValue();

        assertLineEquals(101, primaryFile, "        public ShortcutsChanged(String path, DBusPath sessionHandle, List<ShortcutsChangedShortcutsStruct> shortcuts) throws DBusException {");
        assertLineEquals(99, primaryFile, "        private final List<ShortcutsChangedShortcutsStruct> shortcuts;");
        assertLineEquals(111, primaryFile, "        public List<ShortcutsChangedShortcutsStruct> getShortcuts() {");

        String secondaryFile = analyze.entrySet().stream()
            .filter(e -> e.getKey().getName().equals("ShortcutsChangedShortcutsStruct.java"))
            .findFirst()
            .orElseThrow()
            .getValue();

        assertLineEquals(11, secondaryFile, "    @Position(0)");
        assertLineEquals(12, secondaryFile, "    private final String member0;");
        assertLineEquals(13, secondaryFile, "    @Position(1)");
        assertLineEquals(14, secondaryFile, "    private final Map<String, Variant<?>> member1;");
    }

    @Test
    void testIssue306() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
                new File("src/test/resources/CreateInterface/xdg-desktop/org.freedesktop.impl.portal.Notification.xml"),
                "/", "org.freedesktop.impl.portal.Notification");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(1, analyze.size());

        String clzContent = analyze.get(analyze.keySet().iterator().next());

        assertLineEquals(22, clzContent, "    @DBusBoundProperty(type = PropertySupportedOptionsType.class)");
    }

    @Test
    void testStructFormatting() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
                new File("src/test/resources/CreateInterface/xdg-desktop/org.freedesktop.portal.Usb.xml"),
                "/", "org.freedesktop.portal.Usb");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(6, analyze.size());

        String clzContent = analyze.entrySet().stream()
            .filter(e -> e.getKey().getName().equals("AcquireDevicesDevicesStruct.java"))
            .findFirst()
            .map(e -> e.getValue())
            .orElseThrow();

        assertLineEquals(16, clzContent, "    public AcquireDevicesDevicesStruct(String member0, Map<String, Variant<?>> member1) {");
        assertLineEquals(17, clzContent, "        this.member0 = member0;");
        assertLineEquals(18, clzContent, "        this.member1 = member1;");

        assertLineEquals(21, clzContent, "    public String getMember0() {");
        assertLineEquals(22, clzContent, "        return member0;");

        assertLineEquals(25, clzContent, "    public Map<String, Variant<?>> getMember1() {");
        assertLineEquals(26, clzContent, "        return member1;");
    }

    @Test
    void testTupleFormatting() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(true,
                new File("src/test/resources/CreateInterface/xdg-desktop/org.freedesktop.portal.Documents.xml"),
                "/", "org.freedesktop.portal.Documents");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(4, analyze.size());

        String clzContent = analyze.entrySet().stream()
            .filter(e -> e.getKey().getName().equals("AddFullTuple.java"))
            .findFirst()
            .map(e -> e.getValue())
            .orElseThrow();

        assertLineEquals(14, clzContent, "    public AddFullTuple(A docIds, B extraOut) {");
        assertLineEquals(15, clzContent, "        this.docIds = docIds;");
        assertLineEquals(16, clzContent, "        this.extraOut = extraOut;");

        assertLineEquals(19, clzContent, "    public A getDocIds() {");
        assertLineEquals(20, clzContent, "        return docIds;");

        assertLineEquals(23, clzContent, "    public void setDocIds(A docIds) {");
        assertLineEquals(24, clzContent, "        this.docIds = docIds;");

        assertLineEquals(27, clzContent, "    public B getExtraOut() {");
        assertLineEquals(28, clzContent, "        return extraOut;");

        assertLineEquals(31, clzContent, "    public void setExtraOut(B extraOut) {");
        assertLineEquals(32, clzContent, "        this.extraOut = extraOut;");

    }

    @Test
    void testCreateSampleStructArgs() throws Exception {
        InterfaceCodeGenerator ci2 = loadDBusXmlFile(
                new File("src/test/resources/CreateInterface/sample_struct_args.xml"), "/", "org.example");
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(2, analyze.size()); // class with method and struct class expected

        String clzContent = analyze.get(new File("org", "ExampleMethodExampleArgStruct.java"));

        assertLineEquals(10, clzContent, "    @Position(0)");
        assertLineEquals(11, clzContent, "    private final List<Integer> member0;");
        assertLineEquals(13, clzContent, "    public ExampleMethodExampleArgStruct(List<Integer> member0) {");
        assertLineEquals(17, clzContent, "    public List<Integer> getMember0() {");
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

    static void assertLineEquals(int _lineNo, String _lines, String _compare) {
        assertLineEquals(true, _lineNo, _lines, _compare);
    }

    static void assertLineNotEquals(int _lineNo, String _lines, String _compare) {
        assertLineEquals(false, _lineNo, _lines, _compare);
    }

    /**
     * Assert that the specified line is equal to the compare value.
     * @param _notContains whether the line should or should not contain the compare value
     * @param _lineNo line to compare (zero based)
     * @param _lines string containing lines (will be splitted by line feed)
     * @param _compare compare value
     */
    private static void assertLineEquals(boolean _notContains, int _lineNo, String _lines, String _compare) {
        assertNotNull(_lines, "Lines required");
        assertNotNull(_compare, "Compare line required");

        List<String> list = Arrays.asList(_lines.split("\n"));
        assertFalse(list.isEmpty(), "No lines to compare with");

        if (_lineNo >= list.size()) {
            fail("LineNo " + _lineNo + " is bigger than the available lines " + list.size());
        } else if (_lineNo < 0) {
            fail("LineNo must be >= 0");
        }

        if (_notContains != _compare.equals(list.get(_lineNo))) {
            assertionFailure()
            .message("Line " + _lineNo + " does not match")
            .expected(_compare)
            .actual(list.get(_lineNo))
            .buildAndThrow();
        }

    }
}
