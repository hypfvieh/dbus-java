package org.freedesktop.dbus.utils.generator;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.text.Position;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassConstructor;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassMember;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassMethod;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.github.hypfvieh.util.FileIoUtil;
import com.github.hypfvieh.util.StringUtil;
import com.github.hypfvieh.util.xml.XmlUtil;

/**
 * Replacement for the old CreateInterface tool.
 * This utility class will read introspection data from a given DBus interface
 * and tries to generate proper Java interfaces.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-20
 */
public class InterfaceCodeGenerator {

    private final DocumentBuilderFactory docFac = DocumentBuilderFactory.newInstance();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String nodeName;
    private String busName;

    private String introspectionData;

    public InterfaceCodeGenerator(String _introspectionData, String _objectPath, String _busName) {
        introspectionData = _introspectionData;
        nodeName = _objectPath;
        busName = _busName;
    }


    /**
     * Analyze the DBus interface given in constructor by parsing the introspection data.
     *
     * @param _ignoreDtd true to disable dtd-validation, false otherwise
     * @return List of Filenames and contents for the files
     * @throws Exception on DBUS or IO errors
     */
    public Map<File, String> analyze(boolean _ignoreDtd) throws Exception {
        if (_ignoreDtd) { // if dtd validation is disabled (default)
            docFac.setValidating(false);
            docFac.setNamespaceAware(true);
            docFac.setFeature("http://xml.org/sax/features/namespaces", false);
            docFac.setFeature("http://xml.org/sax/features/validation", false);
            docFac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            docFac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }

        DocumentBuilder builder = docFac.newDocumentBuilder();

        Document document = builder.parse(new InputSource(new StringReader(introspectionData)));

        Element root = document.getDocumentElement();

        if (!StringUtil.isBlank(nodeName) && !StringUtil.isBlank(root.getAttribute("name"))) {
            if (!nodeName.equals(root.getAttribute("name"))) {
                logger.error("Retrieved node '{}' does not match requested node name '{}'!", root.getAttribute("name"), nodeName);
                return null;
            }
        }

        List<Element> interfaceElements = convertToElementList(root.getChildNodes());

        Map<File, String> filesAndContents = new LinkedHashMap<>();


        for (Element ife : interfaceElements) {
            if (!StringUtil.isBlank(busName) && ife.getAttribute("name").startsWith(busName)) {
                filesAndContents.putAll(extractAll(ife));
                continue;
            }
            // take all interfaces
            filesAndContents.putAll(extractAll(ife));
        }

        return filesAndContents;
    }

    /**
     * Converts a NodeList to List&lt;Element&gt;.
     * Will skip all NodeList entries not compatible with Element type.
     *
     * @param _nodeList NodeList to convert
     * @return List of Element, maybe empty
     */
    static List<Element> convertToElementList(org.w3c.dom.NodeList _nodeList) {
        List<org.w3c.dom.Element> elemList = new ArrayList<>();
        for (int i = 0; i < _nodeList.getLength(); i++) {
            if (_nodeList.item(i) instanceof org.w3c.dom.Element) {
                Element elem = (org.w3c.dom.Element) _nodeList.item(i);
                elemList.add(elem);
            }
        }
        return elemList;
    }

    /**
     * Extract all methods/signals etc. from the given interface element.
     *
     * @param _ife interface element
     * @return List of files and their contents
     * @throws IOException when reading xml fails
     * @throws DBusException when DBus fails
     */
    private Map<File, String> extractAll(Element _ife) throws IOException, DBusException {

        String interfaceName = _ife.getAttribute("name");
        Map<DbusInterfaceToFqcn, String> fqcn = DbusInterfaceToFqcn.toFqcn(interfaceName);
        String packageName = fqcn.get(DbusInterfaceToFqcn.PACKAGENAME);
        String className =  fqcn.get(DbusInterfaceToFqcn.CLASSNAME);

        logger.info("Creating interface: {}.{}", packageName, className);

        Map<File, String> filesToCreate = new LinkedHashMap<>();

        ClassBuilderInfo interfaceClass = new ClassBuilderInfo();
        interfaceClass.setClassType(ClassType.INTERFACE);
        interfaceClass.setPackageName(packageName);
        interfaceClass.setDbusPackageName(fqcn.get(DbusInterfaceToFqcn.DBUS_INTERFACE_NAME));
        interfaceClass.setClassName(className);
        interfaceClass.setExtendClass(DBusInterface.class.getName());

        List<ClassBuilderInfo> additionalClasses = new ArrayList<>();

        List<Element> interfaceElements = convertToElementList(_ife.getChildNodes());
        for (Element element : interfaceElements) {
            switch (element.getTagName().toLowerCase()) {
            case "method":
                additionalClasses.addAll(extractMethods(element, interfaceClass));
                break;
            case"signal":
                additionalClasses.addAll(extractSignals(element, interfaceClass));
                break;
            }
        }

        filesToCreate.put(new File(interfaceClass.getFileName()), interfaceClass.createClassFileContent());
        for (ClassBuilderInfo cbi : additionalClasses) {
            filesToCreate.put(new File(cbi.getFileName()), cbi.createClassFileContent());
        }

        return filesToCreate;
    }


    /**
     * Extract &lt;signal&gt; element properties.
     *
     * @param _signalElement signal xml element
     * @param _clzBldr {@link ClassBuilderInfo} object
     * @return List of {@link ClassBuilderInfo} which have been created (maybe empty, never null)
     *
     * @throws IOException on IO Error
     * @throws DBusException on DBus Error
     */
    private List<ClassBuilderInfo> extractSignals(Element _signalElement, ClassBuilderInfo _clzBldr) throws IOException, DBusException {
        List<ClassBuilderInfo> additionalClasses = new ArrayList<>();

        if (!_signalElement.hasChildNodes()) { // signal without any input/output?!
            logger.warn("Signal without any input/output arguments. These are not supported yet, please open a ticket at github!");
            return additionalClasses;
        }

        String className = _signalElement.getAttribute("name");
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf("."));
        }

        ClassBuilderInfo innerClass = new ClassBuilderInfo();
        innerClass.setClassType(ClassType.CLASS);
        innerClass.setExtendClass(DBusSignal.class.getName());
        innerClass.getImports().add(DBusSignal.class.getName());
        innerClass.getImports().add(DBusException.class.getName());
        innerClass.setClassName(className);

        _clzBldr.getInnerClasses().add(innerClass);

        List<Element> signalArgs = XmlUtil.convertToElementList(XmlUtil.applyXpathExpressionToDocument("arg", _signalElement));

        Map<String, String> args = new LinkedHashMap<>();

        int unknownArgCnt = 0;
        for (Element argElm : signalArgs) {
                String argType = TypeConverter.getJavaTypeFromDBusType(argElm.getAttribute("type"), _clzBldr.getImports());
                String argName = StringUtil.snakeToCamelCase(argElm.getAttribute("name"));
                if (StringUtil.isBlank(argName)) {
                    argName = "arg" + unknownArgCnt;
                    unknownArgCnt++;
                }
                args.put(argName, TypeConverter.getProperJavaClass(argType, _clzBldr.getImports()));
        }


        for (Entry<String, String> argEntry : args.entrySet()) {
            innerClass.getMembers().add(new ClassMember(argEntry.getKey(), argEntry.getValue(), true));
        }

        ClassConstructor classConstructor = new ClassBuilderInfo.ClassConstructor();

        Map<String, String> argsMap = new LinkedHashMap<>();
        for (Entry<String, String> e : args.entrySet()) {
            argsMap.put("_" + e.getKey(), e.getValue());
        }
        
        classConstructor.getArguments().putAll(argsMap);
        classConstructor.getThrowArguments().add(DBusException.class.getSimpleName());
        
        classConstructor.getSuperArguments().put("_path", "String");
        classConstructor.getSuperArguments().put("_interfaceName", "String");

        innerClass.getConstructors().add(classConstructor);
        

        return additionalClasses;
    }

    /**
     * Extract &lt;method&gt; elements properties.
     *
     * @param _methodElement method XML element
     * @param _clzBldr {@link ClassBuilderInfo} object
     * @return List of {@link ClassBuilderInfo} which have been created (maybe empty, never null)
     *
     * @throws IOException on IO Error
     * @throws DBusException on DBus Error
     */
    private List<ClassBuilderInfo> extractMethods(Element _methodElement, ClassBuilderInfo _clzBldr) throws IOException, DBusException {

        List<ClassBuilderInfo> additionalClasses = new ArrayList<>();

        if (_methodElement.hasChildNodes()) {
            List<Element> methodArgs = convertToElementList(XmlUtil.applyXpathExpressionToDocument("./arg", _methodElement));

            Map<String, String> inputArgs = new LinkedHashMap<>();
            Map<String, String> outputArgs = new LinkedHashMap<>();

            int unknownArgNameCnt = 0;
            for (Element argElm : methodArgs) {
                String argType;
                String argName = argElm.getAttribute("name");

                if (argElm.getAttribute("type").contains("(")) { // this argument requires some sort of struct
                    String structPart = argElm.getAttribute("type").replaceAll("(\\(.+\\))", "$1");
                    String parentType = buildStructClass(structPart, _methodElement.getAttribute("name") + "Struct", _clzBldr, additionalClasses);
                    if (parentType != null) {
                        argType = parentType;
                    } else {
                        argType = null;
                    }
                } else {
                    argType = TypeConverter.getJavaTypeFromDBusType(argElm.getAttribute("type"), _clzBldr.getImports());
                }

                if (StringUtil.isBlank(argName)) {
                    argName = "arg" + unknownArgNameCnt;
                    unknownArgNameCnt++;
                } else {
                    argName = StringUtil.snakeToCamelCase(argName);
                }

                if ("in".equals(argElm.getAttribute("direction"))) {
                    inputArgs.put(argName, TypeConverter.getProperJavaClass(argType, _clzBldr.getImports()));
                } else if ("out".equals(argElm.getAttribute("direction"))) {
                    outputArgs.put(argName, TypeConverter.getProperJavaClass(argType, _clzBldr.getImports()));
                }
            }

            String resultType;
            if (outputArgs.size() > 1) { // multi-value return
            	logger.debug("Found method with multiple return values: {}", _methodElement.getAttribute("name"));
            	resultType = createTuple(outputArgs, _methodElement.getAttribute("name") + "Tuple", _clzBldr, additionalClasses);
            }
            logger.debug("Found method with arguments: {}({})", _methodElement.getAttribute("name"), inputArgs);
            resultType = outputArgs.isEmpty() ? "void" : outputArgs.get(new ArrayList<>(outputArgs.keySet()).get(0));

            ClassMethod classMethod = new ClassMethod(_methodElement.getAttribute("name"), resultType, false);
            classMethod.getArguments().putAll(inputArgs);
            _clzBldr.getMethods().add(classMethod);

        } else { // method has no arguments

            ClassMethod classMethod = new ClassMethod(_methodElement.getAttribute("name"), "void", false);
            _clzBldr.getMethods().add(classMethod);
        }

        return additionalClasses;

    }

    /**
     * Creates a Tuple extending class to encapsulate a multi-value return (which is not supported by Java natively).
     *
     * @param _outputArgs Map with return arguments (key) and their types (value)
     * @param _className name the tuple class should get
     * @param _parentClzBldr parent class where the tuple was required in
     * @param _additionalClasses list where the new created tuple class will be added to
     * @return FQCN of the newly created tuple based class
     */
    private String createTuple(Map<String, String> _outputArgs, String _className, ClassBuilderInfo _parentClzBldr, List<ClassBuilderInfo> _additionalClasses) {
    	if (_outputArgs == null || _outputArgs.isEmpty() || _additionalClasses == null) {
    		return null;
    	}

    	ClassBuilderInfo info = new ClassBuilderInfo();
    	info.setClassName(_className);
    	info.setPackageName(_parentClzBldr.getPackageName());
    	info.setExtendClass(Tuple.class.getName());

    	if (!_outputArgs.isEmpty()) {
    		info.getImports().add(Position.class.getName());
    	}

    	int position = 0;
    	for (Entry<String, String> entry : _outputArgs.entrySet()) {
    		ClassMember member = new ClassMember(entry.getKey(), entry.getValue(), true);
            member.getAnnotations().add("@Position(" + position + ")");
		}
        ClassConstructor cnstrct = new ClassConstructor();
        cnstrct.getArguments().putAll(_outputArgs);

    	_additionalClasses.add(info);

		return info.getFqcn();
	}


	/**
     * Creates a class for a DBus Struct-Object.
     *
     * @param _dbusTypeStr Dbus Type definition string
     * @param _structName name of the struct to create
     * @param _packageName package name for the struct class
     * @param _structClasses list of {@link ClassCastException}, other struct classes which may be created during struct creation will be added here
     *
     * @return FQCN of the created struct class
     * @throws DBusException on Error
     */
    private String buildStructClass(String _dbusTypeStr, String _structName, ClassBuilderInfo _packageName, List<ClassBuilderInfo> _structClasses) throws DBusException {
        return new StructTreeBuilder().buildStructClasses(_dbusTypeStr, _structName, _packageName, _structClasses);
    }

    /**
     * Creates all files in the given map with the given content in the given directory.
     *
     * @param _outputDir directory to put output files to
     * @param _filesToGenerate map of files and contents which should be created
     */
    static void writeToFile(String _outputDir, Map<File, String> _filesToGenerate) {
        for (Entry<File, String> entry : _filesToGenerate.entrySet()) {
            File outputFile = new File(_outputDir, entry.getKey().getPath());

            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            if (FileIoUtil.writeTextFile(outputFile.getAbsolutePath(), entry.getValue(), Charset.defaultCharset(), false)) {
                LoggerFactory.getLogger(InterfaceCodeGenerator.class).info("Created class file {}", outputFile.getAbsolutePath());
            } else {
                LoggerFactory.getLogger(InterfaceCodeGenerator.class).error("Could not write content to class file {}", outputFile.getName());
            }
        }
    }

    public static void main(String[] args) {

        String busName = null;
        String outputDir = null;
        DBusBusType busType = null;
        boolean ignoreDtd = true;
        String objectPath = null;
        String inputFile = null;

        for (int i = 0; i < args.length; i++) {
            String p = args[i];
            if ("--system".equals(p) || "-y".equals(p)) {
                busType = DBusBusType.SYSTEM;
            } else if ("--session".equals(p) || "-s".equals(p)) {
                busType = DBusBusType.SESSION;
            } else if ("--enable-dtd-validation".equals(p)) {
                ignoreDtd = false;
            } else if ("--help".equals(p) || "-h".equals(p)) {
                printHelp();
                System.exit(0);
            } else if ("--version".equals(p) || "-v".equals(p)) {
                version();
                System.exit(0);
            } else if ("--outputDir".equals(p) || "-o".equals(p)) {
                if (args.length > i) {
                    outputDir = args[++i];
                } else {
                    printHelp();
                    System.exit(0);
                }
            } else if ("--inputFile".equals(p) || "-i".equals(p)) {
                if (args.length > i) {
                    inputFile = args[++i];
                } else {
                    printHelp();
                    System.exit(0);
                }
            } else {
                if (null == busName) {
                    busName = p;
                } else if (null == objectPath) {
                    objectPath = p;
                } else {
                    printHelp();
                    System.exit(1);
                }
            }
        }

        if (objectPath == null) {
            objectPath = "/";
        }
        
        if (outputDir == null) {
            throw new RuntimeException("No output directory (--outputDir) given!");
        }

        Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);

        String introspectionData = null;

        if (!StringUtil.isBlank(busName)) {
            if (!StringUtil.isBlank(inputFile)) {
                File file = new File(inputFile);
                if (!file.exists()) {
                    logger.error("Given input file {} does not exist", file);
                    System.exit(1);
                }
                introspectionData = FileIoUtil.readFileToString(file);
            } else {
                try {
                    logger.info("Introspecting: { Interface: {}, Busname: {} }", objectPath, busName);
    
                    DBusConnection conn = DBusConnection.getConnection(busType);
    
                    Introspectable in = conn.getRemoteObject(busName, objectPath, Introspectable.class);
                    introspectionData = in.Introspect();
                    if (StringUtil.isBlank(introspectionData)) {
                        logger.error("Failed to get introspection data");
                        System.exit(1);
                    }
                    conn.disconnect();
                } catch (DBusExecutionException | DBusException _ex) {
                    logger.error("Failure in DBus Communications. ", _ex);
                    System.exit(1);
    
                }
            }
            
            InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
            try {

                Map<File, String> analyze = ci2.analyze(ignoreDtd);
                writeToFile(outputDir, analyze);
                logger.info("Interface creation finished");
            } catch (Exception _ex) {
                logger.error("Error while analyzing introspection data", _ex);
            }
        } else {
            logger.error("Busname missing!");
            System.exit(1);
        }
    }

    private static void version() {
        System.out.println("Java D-Bus Version " + System.getProperty("Version"));
        System.exit(1);
    }

    private static void printHelp() {
        System.out.println("Syntax: <options> [busname object] [object path]");
        System.out.println("        Options: ");
        System.out.println("        --system           | -y           Use SYSTEM DBus");
        System.out.println("        --session          | -s           Use SESSION DBus");
        System.out.println("        --outputDir <Dir>  | -o <Dir>     Use <Dir> as output directory for all generated files");
        System.out.println("        --inputFile <File> | -i <File>    Use <File> as XML introspection input file instead of querying DBus");
        System.out.println("");
        System.out.println("        --enable-dtd-validation          Enable DTD validation of introspection XML");
        System.out.println("        --version                        Show version information");
        System.out.println("        --help                           Show this help");
    }

    static enum DbusInterfaceToFqcn {
        PACKAGENAME, ORIG_PKGNAME, CLASSNAME, DBUS_INTERFACE_NAME;

        public static Map<DbusInterfaceToFqcn, String> toFqcn(String _interfaceName) {
            String packageName ;
            if (_interfaceName.contains(".")) {
                packageName = _interfaceName.substring(0, _interfaceName.lastIndexOf("."));
            } else {
                packageName = _interfaceName;
            }
            String className = _interfaceName.substring(_interfaceName.lastIndexOf(".") + 1);


            Map<DbusInterfaceToFqcn, String> map = new LinkedHashMap<>();

            map.put(DbusInterfaceToFqcn.CLASSNAME, StringUtil.upperCaseFirstChar(className));
            map.put(DbusInterfaceToFqcn.PACKAGENAME, packageName.toLowerCase());
            
            if (!packageName.equals(packageName.toLowerCase())) {
                map.put(DbusInterfaceToFqcn.ORIG_PKGNAME, packageName);    
                map.put(DbusInterfaceToFqcn.DBUS_INTERFACE_NAME, packageName + "." + className); 
            }
            return map;
        }
    }
}

