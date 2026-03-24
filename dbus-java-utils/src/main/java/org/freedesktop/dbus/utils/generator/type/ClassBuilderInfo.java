package org.freedesktop.dbus.utils.generator.type;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Util;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper to create Java class/interface files with proper formatting.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-22
 */
public class ClassBuilderInfo {

    static final String          DEFAULT_INDENT        = "    ";

    private static final Set<String>     RESERVED_METHOD_NAMES = getReservedMethods(Message.class);

    /** Imported files for this class. */
    private final Set<String>            imports               = new TreeSet<>();
    /** Annotations of this class. */
    private final List<AnnotationInfo>   annotations           = new ArrayList<>();
    /** Members/Fields of this class. */
    private final List<MemberOrArgument> members               = new ArrayList<>();
    /** Interfaces implemented by this class. */
    private final Set<String>            implementedInterfaces = new LinkedHashSet<>();
    /** Methods provided by this class. */
    private final List<ClassMethod>      methods               = new ArrayList<>();
    /** Inner classes inside of this class. */
    private final List<ClassBuilderInfo> innerClasses          = new ArrayList<>();
    /** Constructors for this class. */
    private final List<ClassConstructor> constructors          = new ArrayList<>();

    private final List<String>           generics              = new ArrayList<>();

    /** Prefix to prepend to method/constructor arguments. */
    private final String                 argumentPrefix;

    /** Name of this class. */
    private String                       className;
    /** Package of this class. */
    private String                       packageName;

    /** Package name used by DBus. */
    private String                       dbusPackageName;

    /** Type of this class (interface or class). */
    private ClassType                    classType;
    /** Class which this class may extend. */
    private String                       extendClass;

    /**
     * Create new instance without argument prefix.
     */
    public ClassBuilderInfo() {
        this(null);
    }

    /**
     * Create new instance.
     * @param _argumentPrefix prepend given prefix to all method/constructor arguments
     */
    public ClassBuilderInfo(String _argumentPrefix) {
        argumentPrefix = _argumentPrefix;
    }

    public Set<String> getImports() {
        return imports;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String _packageName) {
        packageName = _packageName;
    }

    public String getDbusPackageName() {
        return dbusPackageName;
    }

    public void setDbusPackageName(String _dbusPackageName) {
        dbusPackageName = _dbusPackageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String _className) {
        className = _className;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType _classType) {
        classType = _classType;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public Set<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public String getExtendClass() {
        return extendClass;
    }

    public void setExtendClass(String _extendClass) {
        extendClass = _extendClass;
    }

    public List<ClassMethod> getMethods() {
        return methods;
    }

    public String getArgumentPrefix() {
        return argumentPrefix;
    }

    public List<MemberOrArgument> getMembers() {
        return members;
    }

    public List<ClassBuilderInfo> getInnerClasses() {
        return innerClasses;
    }

    public List<ClassConstructor> getConstructors() {
        return constructors;
    }

    public List<String> getGenerics() {
        return generics;
    }

    /**
     * Create the Java source for the class information provided.
     *
     * @return String
     */
    public String createClassFileContent() {
        List<String> result = createClassFileContent(false, new LinkedHashSet<>());
        return String.join(System.lineSeparator(), result) + System.lineSeparator();
    }

    /**
     * Create the Java source for the class information provided.
     *
     * @param _staticClass this is static inner class
     * @param _argumentPrefix use given prefix for generated method arguments (null/blank if not needed)
     * @param _otherImports this class needs additional imports (e.g. due to inner class)
     * @return
     */
    private List<String> createClassFileContent(boolean _staticClass, Set<String> _otherImports) {

        final int memberIndentCnt = _staticClass ? 2 : 1;
        final int classIndentCnt = _staticClass ? 1 : 0;
        final String classIndent = ICodeGenerator.INDENT.repeat(classIndentCnt);
        final String memberIndent = ICodeGenerator.INDENT.repeat(memberIndentCnt);

        Set<String> allImports = new TreeSet<>();
        allImports.addAll(getImports());

        if (_otherImports != null) {
            allImports.addAll(_otherImports);
        }

        List<String> content = new ArrayList<>();

        if (!_staticClass) {
            content.add("package " + getPackageName() + ";");
            content.add("");
            content.add("/**");
            content.add(" * Auto-generated class.");
            content.add(" */");

        } else {
            addEmptyLineIfNeeded(content);
        }

        if (getDbusPackageName() != null) {
            allImports.add(DBusInterfaceName.class.getName());
            content.add(classIndent + "@" + DBusInterfaceName.class.getSimpleName() + "(\"" + getDbusPackageName() + "\")");
        }

        for (AnnotationInfo annotation : annotations) {
            allImports.add(annotation.getAnnotationClass().getName());
            allImports.addAll(annotation.getAdditionalImports().stream().map(Class::getName).toList());

            String annotationCode = classIndent + "@" + annotation.getAnnotationClass().getSimpleName();
            if (annotation.getAnnotationParams() != null) {
                annotationCode += "(" + annotation.getAnnotationParams() + ")";
            }
            content.add(annotationCode);
        }

        String bgn = classIndent + "public " + (_staticClass ? "static " : "") + (getClassType() == ClassType.INTERFACE ? "interface" : "class");
        bgn += " " + getClassName();
        if (!getGenerics().isEmpty()) {
            bgn += "<" + String.join(", ", getGenerics()) + ">";
        }
        if (getExtendClass() != null) {
            Set<String> lImports = getImportsForType(getExtendClass());
            getImports().addAll(lImports);
            allImports.addAll(lImports);
            bgn += " extends " + getSimpleTypeClasses(getExtendClass());
        }
        if (!getImplementedInterfaces().isEmpty()) {
            bgn += " implements " + getImplementedInterfaces().stream().map(Util::extractClassNameFromFqcn).collect(Collectors.joining(", "));
            // add classes import if implements-arguments are not a java.lang. classes
            getImports().addAll(getImplementedInterfaces().stream().filter(s -> !s.startsWith("java.lang.")).toList());
        }

        bgn += " {";

        content.add(bgn);
        if (_staticClass) {
            addEmptyLineIfNeeded(content);
        }

        // add member fields
        for (MemberOrArgument member : members) {
            if (!member.getAnnotations().isEmpty()) {
                member.getAnnotations().stream().forEach(l -> {
                   content.add(memberIndent + l.getAnnotationString());
                   allImports.addAll(l.getAdditionalImports().stream().map(Class::getName).toList());
                   allImports.add(l.getAnnotationClass().getName());
                });
            }
            content.add(memberIndent + "private " + member.asOneLineString(false) + ";");
        }

        if (!getConstructors().isEmpty()) {
            for (ClassConstructor constructor : getConstructors()) {
                addEmptyLineIfNeeded(content);
                content.addAll(constructor.generate(1));
            }
        }

        List<ClassMethod> allMethods = new ArrayList<>(getMethods());

        // add getter and setter
        getMembers().stream().map(e -> e.generateMethods(1)).forEach(allMethods::addAll);

        for (ClassMethod mth : allMethods) {
            addEmptyLineIfNeeded(content);
            content.addAll(mth.generate(memberIndentCnt));
        }

        for (ClassBuilderInfo inner : getInnerClasses()) {
            addEmptyLineIfNeeded(content);
            content.addAll(inner.createClassFileContent(true, allImports));
            allImports.addAll(inner.getImports()); // collect additional imports which may have been added by inner class
        }

        addEmptyLineIfNeeded(content);

        content.add(classIndent + "}");

        // write imports to resulting content if this is not a inner class
        if (!_staticClass) {
            content.add(2, "");
            content.addAll(2, allImports.stream()
                    .filter(l -> !l.startsWith("java.lang.")) // do not include imports for 'java.lang'
                    .filter(l -> !l.replaceFirst("(.+)\\..+", "$1").equals(getPackageName())) // do not add imports for classes in same package
                    .filter(l -> l.contains(".")) // no dots in name means this is only a class name so we are in same package and don't need to import
                    .map(l -> "import " + l + ";")
                    .toList());
        } else {
            // add the collected additional imports to the provided set when creating inner class
            _otherImports.addAll(allImports);
        }

        return content;
    }

    static String maybePrefix(String _arg, String _prefix) {
        return Util.isBlank(_prefix) ? _arg : _prefix + _arg;
    }

    static void addEmptyLineIfNeeded(List<String> _content) {
        if (_content == null || _content.isEmpty()) {
            return;
        }

        String lastLine = _content.getLast();
        if (!Util.isBlank(lastLine)) {
            _content.add("");
        }
    }

    /**
     * Create the filename with path this java class should use.
     *
     * @return String, null if class name was null
     */
    public String getFileName() {
        if (getClassName() == null) { // no class name
            return null;
        }
        if (getPackageName() == null) { // no package name
            return getClassName() + ".java";
        }

        return getPackageName().replace(".", File.separator) + File.separator + getClassName() + ".java";
    }

    /**
     * Creates the fully qualified classname based on the provided classname and package.
     *
     * @return String
     */
    public String getFqcn() {
        return Util.isBlank(getPackageName()) ? getClassName() : getPackageName() + "." + getClassName();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [imports=" + imports + ", annotations=" + annotations + ", members=" + members
                + ", implementedInterfaces=" + implementedInterfaces + ", methods=" + methods + ", innerClasses="
                + innerClasses + ", constructors=" + constructors + ", className=" + className + ", packageName="
                + packageName + ", dbusPackageName=" + dbusPackageName + ", classType=" + classType + ", extendClass="
                + extendClass + "]";
    }

    /**
     * Simplify class names in the type. Please go to unit tests for usage examples.
     *
     * @param _type type described in the string format
     * @return type with simplified class names
     */
    public static String getSimpleTypeClasses(String _type) {
        if (_type == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Pattern compile = Pattern.compile("([^, <>?]+)");
        Matcher matcher = compile.matcher(_type);
        while (matcher.find()) {
            String match = matcher.group();
            matcher.appendReplacement(sb, Util.extractClassNameFromFqcn(match));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Get all classes that should be imported for the input type, this method works fine with generic types.
     * Please go to unit tests for usage examples.
     *
     * @param _type type described in the string format
     * @return set of classes required for imports
     */
    public static Set<String> getImportsForType(String _type) {
        Set<String> imports = new HashSet<>();
        if (!_type.contains("<")) {
            if (!_type.startsWith("java.lang.") && _type.contains(".")) {
                imports.add(_type);
            }
            return imports;
        }
        Pattern compile = Pattern.compile("([^, <>?]+)");
        Matcher matcher = compile.matcher(_type);
        while (matcher.find()) {
            String match = matcher.group();

            if (!match.startsWith("java.lang.") && match.contains(".")) {
                imports.add(match);
            }
        }
        return imports;
    }

    static Set<String> getReservedMethods(Class<?> _class) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(_class).getMethodDescriptors())
                .map(e -> e.getMethod().getName())
                .collect(Collectors.toSet());
        } catch (IntrospectionException _ex) {
            LoggerFactory.getLogger(ClassBuilderInfo.class).error("Could not extract method names from {}", _class, _ex);
            return Set.of();
        }
    }

    /**
     * Check if the provided method name classes with any method name found in {@link #RESERVED_METHOD_NAMES}.
     * Will check the given method name with usual Java method prefixes (get/is/set) as well.
     * @param _methodName method name
     * @return true if reserved
     */
    static boolean isReservedMethodName(String _methodName) {
        return RESERVED_METHOD_NAMES.contains(_methodName) || RESERVED_METHOD_NAMES.contains("set" + _methodName)
            || RESERVED_METHOD_NAMES.contains("get" + _methodName) || RESERVED_METHOD_NAMES.contains("is" + _methodName);
    }

    /**
     * Enum to define either the {@link ClassBuilderInfo} is for a CLASS or an INTERFACE.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public enum ClassType {
        INTERFACE,
        CLASS;
    }

}
