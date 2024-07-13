package org.freedesktop.dbus.utils.generator;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.utils.Util;
import org.freedesktop.dbus.utils.XmlUtil;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
    private final Logger                 logger = LoggerFactory.getLogger(getClass());

    private final String                 nodeName;
    private final String                 busName;

    private final String                 introspectionData;

    private final boolean                disableFilter;
    private final String                 forcePackageName;
    private final boolean                propertyMethods;

    private final Set<String>            generatedStructClassNames;

    public InterfaceCodeGenerator(boolean _disableFilter, String _introspectionData, String _objectPath, String _busName, String _packageName, boolean _propertyMethods) {
        disableFilter = _disableFilter;
        introspectionData = _introspectionData;
        nodeName = _objectPath;
        busName = Util.isBlank(_busName) ? "*" : _busName;
        forcePackageName = _packageName;
        propertyMethods = _propertyMethods;
        generatedStructClassNames = new LinkedHashSet<>();
        logger.debug("ForcePackageName: {} / PropertyMethods: {}", forcePackageName, propertyMethods);
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

        if (!Util.isBlank(nodeName) && !Util.isBlank(root.getAttribute("name")) && !nodeName.equals(root.getAttribute("name"))) {
            logger.atError()
                .addArgument(() -> root.getAttribute("name"))
                .addArgument(nodeName)
                .log("Retrieved node '{}' does not match requested node name '{}'!");
            return null;
        }

        List<Element> interfaceElements = convertToElementList(root.getChildNodes());

        Map<File, String> filesAndContents = new LinkedHashMap<>();

        boolean noBusnameGiven = "*".equals(busName) || disableFilter;

        for (Element ife : interfaceElements) {
            String nameAttrib = ife.getAttribute("name");
            if (disableFilter && ("org.freedesktop.DBus.Introspectable".equals(nameAttrib)
                || "org.freedesktop.DBus.Properties".equals(nameAttrib))) {
                continue; // do not create DBus classes (they are part of dbus-java)
            }
            if (!noBusnameGiven && !nameAttrib.startsWith(busName)) { // busname was set, and current element does not match -> skip
                logger.info("Skipping: {} - does not match given busName: {}", nameAttrib, busName);
                continue;
            } else if (noBusnameGiven) { // no busname given, take all
                // take all interfaces
                filesAndContents.putAll(extractAll(ife));
                continue;
            }
            // busname given and matching
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
    static List<Element> convertToElementList(NodeList _nodeList) {
        List<Element> elemList = new ArrayList<>();
        for (int i = 0; i < _nodeList.getLength(); i++) {
            if (_nodeList.item(i) instanceof Element elm) {
                elemList.add(elm);
            }
        }
        return elemList;
    }

    /**
     * Extract all methods/signals etc. from the given interface element.
     *
     * @param _ife interface element
     * @return Map of files and their contents
     * @throws IOException when reading xml fails
     * @throws DBusException when DBus fails
     */
    private Map<File, String> extractAll(Element _ife) throws IOException, DBusException {

        String interfaceName = _ife.getAttribute("name");
        Map<DbusInterfaceToFqcn, String> fqcn = DbusInterfaceToFqcn.toFqcn(interfaceName);
        String originalPackageName = fqcn.get(DbusInterfaceToFqcn.PACKAGENAME);
        String packageName =  forcePackageName == null ? originalPackageName : forcePackageName;
        String className = fqcn.get(DbusInterfaceToFqcn.CLASSNAME);

        logger.info("Creating interface: {}.{}", packageName, className);

        final Map<File, String> filesToCreate = new LinkedHashMap<>();

        ClassBuilderInfo interfaceClass = new ClassBuilderInfo();
        interfaceClass.setClassType(ClassType.INTERFACE);
        interfaceClass.setPackageName(packageName);
        interfaceClass.setDbusPackageName(fqcn.get(DbusInterfaceToFqcn.DBUS_INTERFACE_NAME));
        interfaceClass.setClassName(className);
        if (forcePackageName != null) {
            interfaceClass.getAnnotations().add(new AnnotationInfo(DBusInterfaceName.class,
                "\"" + originalPackageName + "." + className + "\""));
        }
        interfaceClass.setExtendClass(DBusInterface.class.getName());

        List<ClassBuilderInfo> additionalClasses = new ArrayList<>();

        List<Element> interfaceElements = convertToElementList(_ife.getChildNodes());
        for (Element element : interfaceElements) {
            switch (element.getTagName().toLowerCase()) {
                case "method"   -> additionalClasses.addAll(extractMethods(element, interfaceClass));
                case "property" -> additionalClasses.addAll(extractProperties(element, interfaceClass));
                case "signal"   -> additionalClasses.addAll(extractSignals(element, interfaceClass));
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
            className = className.substring(className.lastIndexOf('.'));
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
            String argName = Util.snakeToCamelCase(argElm.getAttribute("name"));
            if (Util.isBlank(argName)) {
                argName = "arg" + unknownArgCnt;
                unknownArgCnt++;
            }
            args.put(argName, TypeConverter.getProperJavaClass(argType, _clzBldr.getImports()));
        }

        for (Entry<String, String> argEntry : args.entrySet()) {
            innerClass.getMembers().add(new MemberOrArgument(argEntry.getKey(), argEntry.getValue(), true));
        }

        ClassConstructor classConstructor = new ClassBuilderInfo.ClassConstructor();

        List<MemberOrArgument> argsList = new ArrayList<>();
        for (Entry<String, String> e : args.entrySet()) {
            argsList.add(new MemberOrArgument("_" + e.getKey(), e.getValue(), false));
        }

        classConstructor.getArguments().addAll(argsList);
        classConstructor.getThrowArguments().add(DBusException.class.getSimpleName());

        classConstructor.getSuperArguments().add(new MemberOrArgument("_path", "String", false));
        classConstructor.getSuperArguments().addAll(argsList);

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

        String methodElementName = _methodElement.getAttribute("name");
        if (_methodElement.hasChildNodes()) {
            List<Element> methodArgs = convertToElementList(XmlUtil.applyXpathExpressionToDocument("./arg", _methodElement));

            List<MemberOrArgument> inputArgs = new ArrayList<>();
            List<MemberOrArgument> outputArgs = new ArrayList<>();

            List<String> dbusOutputArgTypes = new ArrayList<>();

            int unknownArgNameCnt = 0;
            for (Element argElm : methodArgs) {
                String argType;
                String argName = argElm.getAttribute("name");

                if (argElm.getAttribute("type").contains("(")) { // this argument requires some sort of struct
                    String structPart = argElm.getAttribute("type").replaceAll("(\\(.+\\))", "$1");
                    String paramName = Util.defaultString(Util.upperCaseFirstChar(Util.snakeToCamelCase(argName)), "");
                    String parentType = buildStructClass(structPart, methodElementName + paramName + "Struct", _clzBldr, additionalClasses);
                    if (parentType != null) {
                        argType = parentType;
                    } else {
                        argType = null;
                    }
                } else {
                    argType = TypeConverter.getJavaTypeFromDBusType(argElm.getAttribute("type"), _clzBldr.getImports());
                }

                if (Util.isBlank(argName)) {
                    argName = "_arg" + unknownArgNameCnt;
                    unknownArgNameCnt++;
                } else {
                    argName = Util.snakeToCamelCase(argName);
                }

                String dirAttr = argElm.getAttribute("direction");
                if ("in".equals(dirAttr) || "".equals(dirAttr)) {
                    inputArgs.add(new MemberOrArgument(argName, TypeConverter.getProperJavaClass(argType, _clzBldr.getImports())));
                } else if ("out".equals(dirAttr)) {
                    outputArgs.add(new MemberOrArgument(argName, TypeConverter.getProperJavaClass(argType, _clzBldr.getImports()), false));
                    dbusOutputArgTypes.add(argType);
                }
            }

            String resultType;
            if (outputArgs.size() > 1) { // multi-value return
                logger.debug("Found method with multiple return values: {}", methodElementName);
                resultType = createTuple(outputArgs, methodElementName + "Tuple", _clzBldr, additionalClasses, dbusOutputArgTypes);
            } else {
                logger.debug("Found method with arguments: {}({})", methodElementName, inputArgs);
                resultType = outputArgs.isEmpty() ? "void" : outputArgs.get(0).getFullType(new HashSet<>());
            }

            ClassMethod classMethod = new ClassMethod(methodElementName, resultType, false);
            classMethod.getArguments().addAll(inputArgs);

            _clzBldr.getMethods().add(classMethod);

        } else { // method has no arguments

            ClassMethod classMethod = new ClassMethod(methodElementName, "void", false);
            _clzBldr.getMethods().add(classMethod);
        }

        return additionalClasses;

    }

    /**
     * Extract &lt;property&gt; elements properties.
     *
     * @param _propertyElement method XML element
     * @param _clzBldr {@link ClassBuilderInfo} object
     * @return List of {@link ClassBuilderInfo} which have been created (maybe empty, never null)
     * @throws DBusException on DBus Error
     */
    private List<ClassBuilderInfo> extractProperties(Element _propertyElement, ClassBuilderInfo _clzBldr) throws DBusException {
        List<ClassBuilderInfo> additionalClasses = new ArrayList<>();

        String attrName = _propertyElement.getAttribute("name");
        String attrAccess = _propertyElement.getAttribute("access");
        String attrType = _propertyElement.getAttribute("type");

        String access;
        if (DBusProperty.Access.READ.getAccessName().equals(attrAccess)) {
            access = DBusProperty.Access.READ.name();
        } else if (DBusProperty.Access.WRITE.getAccessName().equals(attrAccess)) {
            access = DBusProperty.Access.WRITE.name();
        } else {
            access = DBusProperty.Access.READ_WRITE.name();
        }
        _clzBldr.getImports().add(DBusProperty.Access.class.getCanonicalName());

        String type;
        boolean isStruct = false;
        if ("av".equals(attrType)) {
            // raw type list
            type = List.class.getName();
            _clzBldr.getImports().add(type);
        } else if ("a{vv}".equals(attrType)) {
            // raw type map
            type = Map.class.getName();
            _clzBldr.getImports().add(type);
        } else if (attrType.contains("(")) {
            // contains structure
            String structPart = attrType.replaceAll("(\\(.+\\))", "$1");
            type = buildStructClass(structPart, "Property" + attrName + "Struct", _clzBldr, additionalClasses);
            isStruct = true;
        } else {
            type = TypeConverter.getJavaTypeFromDBusType(attrType, _clzBldr.getImports());
        }
        if (type == null) {
            type = Variant.class.getName();
        }
        type = type.replaceAll(CharSequence.class.getName(), String.class.getName());
        boolean isComplex = type.contains("<");

        String clzzName;
        ClassBuilderInfo propertyTypeRef = null;
        String origType = null;
        if (!isComplex) {
            clzzName = ClassBuilderInfo.getClassName(type);
        } else {
            origType = type;
            type = TypeRef.class.getName() + "<" + type + ">";
            String typeRefInterfaceName = "Property" + attrName + "Type";
            propertyTypeRef = new ClassBuilderInfo();
            propertyTypeRef.setClassType(ClassType.INTERFACE);
            propertyTypeRef.setClassName(typeRefInterfaceName);
            propertyTypeRef.setExtendClass(type);
            _clzBldr.getInnerClasses().add(propertyTypeRef);
            clzzName = _clzBldr.getClassName() + "." + typeRefInterfaceName;
        }

        if (propertyMethods) {
            if (DBusProperty.Access.READ.getAccessName().equals(attrAccess)
                || DBusProperty.Access.READ_WRITE.getAccessName().equals(attrAccess)) {

                String rtnType = origType != null ? origType : clzzName;

                ClassMethod classMethod = new ClassMethod(
                    ("boolean".equalsIgnoreCase(clzzName) ? "is" : "get") + attrName, rtnType, false);
                _clzBldr.getMethods().add(classMethod);

                if (propertyTypeRef != null) {
                    classMethod.getAnnotations().add("@" + DBusBoundProperty.class.getSimpleName() + "(type = " + propertyTypeRef.getClassName() + ".class)");
                } else if (isStruct) {
                    classMethod.getAnnotations().add("@" + DBusBoundProperty.class.getSimpleName() + "(type = " + clzzName + ".class)");
                } else {
                    classMethod.getAnnotations().add("@" + DBusBoundProperty.class.getSimpleName());
                }
                _clzBldr.getImports().add(DBusBoundProperty.class.getName());
            }

            if (DBusProperty.Access.WRITE.getAccessName().equals(attrAccess)
                || DBusProperty.Access.READ_WRITE.getAccessName().equals(attrAccess)) {
                ClassMethod classMethod = new ClassMethod("set" + attrName, "void", false);
                classMethod.getArguments().add(new MemberOrArgument(attrName.substring(0, 1).toLowerCase() + attrName.substring(1), clzzName));
                    _clzBldr.getMethods().add(classMethod);
                classMethod.getAnnotations().add("@" + DBusBoundProperty.class.getSimpleName());
                _clzBldr.getImports().add(DBusBoundProperty.class.getName());
            }
        } else {
            String annotationParams = "name = \"" + attrName + "\", "
                + "type = " + clzzName + ".class, "
                + "access = " + DBusProperty.Access.class.getSimpleName() + "." + access;

            AnnotationInfo annotationInfo = new AnnotationInfo(DBusProperty.class, annotationParams);
            _clzBldr.getAnnotations().add(annotationInfo);
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
     * @param _dbusOutputArgTypes Dbus argument names and data types
     * @return FQCN of the newly created tuple based class
     */
    private String createTuple(List<MemberOrArgument> _outputArgs, String _className, ClassBuilderInfo _parentClzBldr, List<ClassBuilderInfo> _additionalClasses, List<String> _dbusOutputArgTypes) {
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

        ArrayList<MemberOrArgument> cnstrctArgs = new ArrayList<>();
        int position = 0;
        for (MemberOrArgument entry : _outputArgs) {
            entry.getAnnotations().add("@Position(" + position++ + ")");
            cnstrctArgs.add(new MemberOrArgument(entry.getName(), entry.getType()));
        }

        for (String outputType : _dbusOutputArgTypes) {
            for (String part : outputType.replace(" ", "").replace(">", "").split("<|,")) {
                info.getImports().add(part);
            }
        }

        ClassConstructor cnstrct = new ClassConstructor();
        cnstrct.getArguments().addAll(cnstrctArgs);

        info.getConstructors().add(cnstrct);
        info.getMembers().addAll(_outputArgs);
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
        String structFqcn = _packageName.getPackageName() + "." + Util.upperCaseFirstChar(_structName);
        String structName = _structName;
        if (generatedStructClassNames.contains(structFqcn)) {
            while (generatedStructClassNames.contains(structFqcn)) {
                structFqcn += "Struct";
                structName += "Struct";
            }
        }
        String structClassName = new StructTreeBuilder().buildStructClasses(_dbusTypeStr, structName, _packageName, _structClasses);
        generatedStructClassNames.add(structFqcn);
        return structClassName;
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

            if (Util.writeTextFile(outputFile.getAbsolutePath(), entry.getValue(), Charset.defaultCharset(), false)) {
                LoggerFactory.getLogger(InterfaceCodeGenerator.class).info("Created class file {}", outputFile.getAbsolutePath());
            } else {
                LoggerFactory.getLogger(InterfaceCodeGenerator.class).error("Could not write content to class file {}", outputFile.getName());
            }
        }
    }

    public static void main(String[] _args) {

        String busName = null;
        String outputDir = null;
        DBusBusType busType = null;
        boolean ignoreDtd = true;
        String objectPath = null;
        String inputFile = null;
        boolean noFilter = false;
        boolean propertyMethods = false;
        String forcePackageName = null;

        for (int i = 0; i < _args.length; i++) {
            String p = _args[i];
            if ("--system".equals(p) || "-y".equals(p)) {
                busType = DBusBusType.SYSTEM;
            } else if ("--session".equals(p) || "-s".equals(p)) {
                busType = DBusBusType.SESSION;
            } else if ("--enable-dtd-validation".equals(p)) {
                ignoreDtd = false;
            } else if ("--help".equals(p) || "-h".equals(p)) {
                printHelp();
                System.exit(0);
            } else if ("--all".equals(p) || "-a".equals(p)) {
                noFilter = true;
            } else if ("--propertyMethods".equals(p) || "-m".equals(p)) {
                propertyMethods = true;
            } else if ("--package".equals(p) || "-p".equals(p)) {
                if (_args.length > i) {
                    forcePackageName = _args[++i];
                } else {
                    printHelp();
                    System.exit(0);
                }
            } else if ("--version".equals(p) || "-v".equals(p)) {
                version();
                System.exit(0);
            } else if ("--outputDir".equals(p) || "-o".equals(p)) {
                if (_args.length > i) {
                    outputDir = _args[++i];
                } else {
                    printHelp();
                    System.exit(0);
                }
            } else if ("--inputFile".equals(p) || "-i".equals(p)) {
                if (_args.length > i) {
                    inputFile = _args[++i];
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

        if (!Util.isBlank(inputFile)) {
            File file = new File(inputFile);
            if (!file.exists()) {
                logger.error("Given input file {} does not exist", file);
                System.exit(1);
            }
            introspectionData = Util.readFileToString(file);
        } else if (!Util.isBlank(busName)) {
            logger.info("Introspecting: { Interface: {}, Busname: {} }", objectPath, busName);

            try (DBusConnection conn = DBusConnectionBuilder.forType(busType).build()) {
                Introspectable in = conn.getRemoteObject(busName, objectPath, Introspectable.class);
                introspectionData = in.Introspect();
                if (Util.isBlank(introspectionData)) {
                    logger.error("Failed to get introspection data");
                    System.exit(1);
                }
            } catch (DBusExecutionException | DBusException | IOException _ex) {
                logger.error("Failure in DBus Communications. ", _ex);
                System.exit(1);
            }
        } else {
            logger.error("Busname missing!");
            System.exit(1);
        }

        InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(noFilter, introspectionData, objectPath, busName, forcePackageName, propertyMethods);
        try {

            Map<File, String> analyze = ci2.analyze(ignoreDtd);
            if (analyze == null) {
                logger.error("Unable to create interface files");
                return;
            }
            if (analyze.isEmpty()) {
                logger.warn("No files to create!");
            }
            writeToFile(outputDir, analyze);
            logger.info("Interface creation finished");
        } catch (Exception _ex) {
            logger.error("Error while analyzing introspection data", _ex);
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
        System.out.println("        --packageName <Pkg>| -p <Pkg>     Use <Pkg> as the Java package instead of using the DBus namespace.");
        System.out.println("        --inputFile <File> | -i <File>    Use <File> as XML introspection input file instead of querying DBus");
        System.out.println("        --all              | -a           Create all classes for given bus name (do not filter)");
        System.out.println("        --boundProperties  | -b           Generate setter/getter methods for properties");
        System.out.println("");
        System.out.println("        --enable-dtd-validation          Enable DTD validation of introspection XML");
        System.out.println("        --version                        Show version information");
        System.out.println("        --help                           Show this help");
        System.out.println("");
        System.out.println("If --inputFile is given busname object argument can be skipped (or * can be used), that will force the util to extract all interfaces found in the given file.");
        System.out.println("If busname (not empty, blank and not '*') is given, then only interfaces starting with the given busname will be extracted.");
    }

    enum DbusInterfaceToFqcn {
        PACKAGENAME,
        ORIG_PKGNAME,
        CLASSNAME,
        DBUS_INTERFACE_NAME;

        public static Map<DbusInterfaceToFqcn, String> toFqcn(String _interfaceName) {
            String packageName;
            if (_interfaceName.contains(".")) {
                packageName = _interfaceName.substring(0, _interfaceName.lastIndexOf('.'));
            } else {
                packageName = _interfaceName;
            }
            String className = _interfaceName.substring(_interfaceName.lastIndexOf('.') + 1);

            Map<DbusInterfaceToFqcn, String> map = new LinkedHashMap<>();

            map.put(CLASSNAME, Util.upperCaseFirstChar(className));
            map.put(PACKAGENAME, packageName.toLowerCase());

            if (!packageName.equals(packageName.toLowerCase())) {
                map.put(ORIG_PKGNAME, packageName);
                map.put(DBUS_INTERFACE_NAME, packageName + "." + className);
            }
            return map;
        }
    }

}
