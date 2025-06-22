package org.freedesktop.dbus.utils.generator;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.utils.Util;
import org.freedesktop.dbus.utils.bin.IdentifierMangler;

import java.io.File;
import java.lang.annotation.Annotation;
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

    private static final String DEFAULT_INDENT = "    ";
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

    public List<MemberOrArgument> getMembers() {
        return members;
    }

    public List<ClassBuilderInfo> getInnerClasses() {
        return innerClasses;
    }

    public List<ClassConstructor> getConstructors() {
        return constructors;
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
        List<String> content = new ArrayList<>();

        final String classIndent = _staticClass ? DEFAULT_INDENT : "";
        final String memberIndent = _staticClass ? DEFAULT_INDENT.repeat(2) : DEFAULT_INDENT;

        Set<String> allImports = new TreeSet<>();
        allImports.addAll(getImports());
        if (_otherImports != null) {
            allImports.addAll(_otherImports);
        }

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
            String annotationCode = classIndent + "@" + annotation.getAnnotationClass().getSimpleName();
            if (annotation.getAnnotationParams() != null) {
                annotationCode += "(" + annotation.getAnnotationParams() + ")";
            }
            content.add(annotationCode);
        }

        String bgn = classIndent + "public " + (_staticClass ? "static " : "") + (getClassType() == ClassType.INTERFACE ? "interface" : "class");
        bgn += " " + getClassName();
        if (getExtendClass() != null) {
            Set<String> lImports = getImportsForType(getExtendClass());
            getImports().addAll(lImports);
            allImports.addAll(lImports);
            bgn += " extends " + getSimpleTypeClasses(getExtendClass());
        }
        if (!getImplementedInterfaces().isEmpty()) {
            bgn += " implements " + getImplementedInterfaces().stream().map(ClassBuilderInfo::getClassName).collect(Collectors.joining(", "));
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
                content.addAll(member.getAnnotations().stream().map(l -> memberIndent + l).toList());
            }
            content.add(memberIndent + "private " + member.asOneLineString(allImports, "", false) + ";");
        }

        if (!getConstructors().isEmpty()) {
            for (ClassConstructor constructor : getConstructors()) {
                addEmptyLineIfNeeded(content);
                String outerIndent = _staticClass ? DEFAULT_INDENT.repeat(2) : DEFAULT_INDENT;
                content.addAll(constructor.generatedCode(outerIndent, getClassName(), argumentPrefix, allImports));
            }
        }

        // add getter and setter
        for (MemberOrArgument member : members) {
            addEmptyLineIfNeeded(content);
            content.addAll(member.generateCode(memberIndent, argumentPrefix, allImports));
        }

        for (ClassMethod mth : getMethods()) {
            addEmptyLineIfNeeded(content);
            content.addAll(mth.generateCode(getClassType() == ClassType.INTERFACE, argumentPrefix, memberIndent, allImports));
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
        }

        return content;
    }

    private static String maybePrefix(String _arg, String _prefix) {
        return Util.isBlank(_prefix) ? _arg : _prefix + _arg;
    }

    private static void addEmptyLineIfNeeded(List<String> _content) {
        if (_content == null || _content.isEmpty()) {
            return;
        }

        String lastLine = _content.get(_content.size() - 1);
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
     * Extract the class name from a given FQCN (fully qualified classname).
     *
     * @param _fqcn fqcn to analyze
     * @return classname, null if input was null
     */
    static String getClassName(String _fqcn) {
        if (_fqcn == null) {
            return null;
        }

        String clzzName = _fqcn;
        if (clzzName.contains(".")) {
            clzzName = clzzName.substring(clzzName.lastIndexOf('.') + 1);
        }
        return clzzName;
    }

    /**
     * Simplify class names in the type. Please go to unit tests for usage examples.
     *
     * @param _type type described in the string format
     * @return type with simplified class names
     */
    static String getSimpleTypeClasses(String _type) {
        if (_type == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Pattern compile = Pattern.compile("([^, <>?]+)");
        Matcher matcher = compile.matcher(_type);
        while (matcher.find()) {
            String match = matcher.group();
            matcher.appendReplacement(sb, getClassName(match));
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
    static Set<String> getImportsForType(String _type) {
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

    /**
     * Contains information about annotation to place on classes, members or methods.
     *
     * @author hypfvieh
     * @since v3.2.1 - 2019-11-13
     */
    public static class AnnotationInfo {
        /** Annotation class. */
        private final Class<? extends Annotation> annotationClass;
        /** Annotation params (e.g. value = "foo", key = "bar"). */
        private final String                      annotationParams;

        public AnnotationInfo(Class<? extends Annotation> _annotationClass, String _annotationParams) {
            annotationClass = _annotationClass;
            annotationParams = _annotationParams;
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return annotationClass;
        }

        public String getAnnotationParams() {
            return annotationParams;
        }
    }

    /**
     * Pojo which represents a class method.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class ClassMethod {

        private static final String METHOD_TEMPL = """
            %s%s %s(%s);
            """;

        /** Name of this method. */
        private final String                 name;
        /** Return value of the method. */
        private final String                 returnType;
        /** True if method should be final, false otherwise. */
        private final boolean                finalMethod;
        /** Arguments for this method, key is argument name, value is argument type. */
        private final List<MemberOrArgument> arguments   = new ArrayList<>();
        /** List of annotations for this method. */
        private final List<String>           annotations = new ArrayList<>();

        public ClassMethod(String _name, String _returnType, boolean _finalMethod) {
            name = _name;
            returnType = _returnType;
            finalMethod = _finalMethod;
        }

        public String getName() {
            return name;
        }

        public String getReturnType() {
            return returnType;
        }

        public boolean isFinalMethod() {
            return finalMethod;
        }

        public List<MemberOrArgument> getArguments() {
            return arguments;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public List<String> generateCode(boolean _isInterface, String _argumentPrefix, String _indent, Set<String> _allImports) {
            List<String> result = new ArrayList<>();
            if (!getAnnotations().isEmpty()) {
                result.addAll(getAnnotations().stream().map(a -> _indent + a).toList());
            }

            String publicModifier = !_isInterface ? "public " : "";
            String mthReturnType = getReturnType() == null ? "void"
                : TypeConverter.getProperJavaClass(getReturnType(), _allImports);
            String args = "";
            if (!getArguments().isEmpty()) {
                args += getArguments().stream()
                    .map(e -> e.asOneLineString(_allImports, _argumentPrefix, true))
                        .collect(Collectors.joining(", "));
            }

            METHOD_TEMPL.formatted(publicModifier, mthReturnType, getName(), args)
                .lines().map(l -> _indent + l).forEach(result::add);

            return result;
        }

    }

    /**
     * Pojo which represents a class member/field or argument.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class MemberOrArgument {

        private static final String GETTER_TEMPL = """
            public %s get%s() {
            %sreturn %s;
            }
            """;

        private static final String SETTER_TEMPL = """
            public void set%s(%s %s) {
            %s%s = %s;
            }
            """;

        /** Name of member/field. */
        private final String       name;
        /** Type of member/field (e.g. String, int...). */
        private String       type;
        /** True to force this member to be final, false otherwise. */
        private final boolean      finalArg;
        /** List of classes/types or placeholders put into diamond operators to use as generics. */
        private final List<String> generics    = new ArrayList<>();
        /** List of annotations for this member. */
        private final List<String> annotations = new ArrayList<>();

        public MemberOrArgument(String _name, String _type, boolean _finalMember) {
            // repair reserved words by adding 'Param' as appendix, and when start with _ too
            name = (IdentifierMangler.isReservedWord(_name) || IdentifierMangler.isReservedWord(_name.replaceFirst("^_(.+)", "$1"))) ? _name + "param" : _name;
            type = _type;
            finalArg = _finalMember;
        }

        public MemberOrArgument(String _name, String _type) {
            this(_name, _type, false);
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public void setType(String _type) {
            type = _type;
        }

        public boolean isFinalArg() {
            return finalArg;
        }

        public List<String> getGenerics() {
            return generics;
        }

        public String getFullType(Set<String> _allImports) {
            StringBuilder sb = new StringBuilder();
            sb.append(TypeConverter.getProperJavaClass(getType(), _allImports));

            if (!getGenerics().isEmpty()) {
                sb.append("<")
                        .append(getGenerics().stream().map(c -> TypeConverter.convertJavaType(c, false)).collect(Collectors.joining(", ")))
                        .append(">");
            }

            return sb.toString();
        }

        public String asOneLineString(Set<String> _allImports, String _prefix, boolean _includeAnnotations) {
            StringBuilder sb = new StringBuilder();

            if (isFinalArg()) {
                sb.append("final ");
            }

            if (_includeAnnotations && !getAnnotations().isEmpty()) {
                sb.append(String.join(" ", getAnnotations()))
                        .append(" ");
            }

            sb.append(getFullType(_allImports));

            sb.append(" ");

            sb.append(maybePrefix(getName(), _prefix));

            return sb.toString();
        }

        public List<String> generateCode(String _indent, String _prefix, Set<String> _allImports) {
            List<String> result = new ArrayList<>();
            String memberType = TypeConverter.getProperJavaClass(getType(), _allImports);

            if (!getGenerics().isEmpty()) {
                memberType += "<" + getGenerics().stream().map(c -> TypeConverter.convertJavaType(c, false)).collect(Collectors.joining(", ")) + ">";
            }

            String getterSetterName = Util.snakeToCamelCase(Util.upperCaseFirstChar(getName()));
            if (!isFinalArg()) {
                SETTER_TEMPL.formatted(getterSetterName, memberType,
                    maybePrefix("arg", _prefix), DEFAULT_INDENT, getName(), maybePrefix("arg", _prefix))
                        .lines().map(l -> _indent + l).forEach(result::add);
            }

            addEmptyLineIfNeeded(result);
            GETTER_TEMPL.formatted(memberType, getterSetterName, DEFAULT_INDENT, getName())
                .lines().map(l -> _indent + l).forEach(result::add);

            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [name=" + name + ", type=" + type + ", finalArg=" + finalArg + ", generics="
                    + generics + ", annotations=" + annotations + "]";
        }

    }

    /**
     * Pojo which represents a class constructor.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class ClassConstructor {
        private static final String          CONSTRUCTOR_TEMPL = """
            public %s(%s)%s {
            %s
            }
            """;

        /** Map of arguments for the constructor. Key is argument name, value is argument type. */
        private final List<MemberOrArgument> arguments      = new ArrayList<>();
        /** Map of arguments for the super-constructor. Key is argument name, value is argument type. */
        private final List<MemberOrArgument> superArguments = new ArrayList<>();

        /** List of throws arguments. */
        private final List<String>           throwArguments = new ArrayList<>();

        public List<String> getThrowArguments() {
            return throwArguments;
        }

        public List<MemberOrArgument> getArguments() {
            return arguments;
        }

        public List<MemberOrArgument> getSuperArguments() {
            return superArguments;
        }

        public String argumentsAsString(Set<String> _allImports, String _prefix) {
            return getArguments().stream().map(a -> a.asOneLineString(_allImports, _prefix, true)).collect(Collectors.joining(", "));
        }

        public List<String> generatedCode(String _indent, String _className, String _argumentPrefix, Set<String> _allImports) {

            List<ClassBuilderInfo.MemberOrArgument> filteredSuperArguments = new ArrayList<>(getSuperArguments());
            filteredSuperArguments.removeIf(e -> getArguments().contains(e));
            String constructorArgs = "";

            if (!filteredSuperArguments.isEmpty()) {
                constructorArgs += filteredSuperArguments.stream().map(e -> e.asOneLineString(_allImports, _argumentPrefix, false)).collect(Collectors.joining(", "));
                if (!getArguments().isEmpty()) {
                    constructorArgs += ", ";
                }
            }

            if (!getArguments().isEmpty()) {
                constructorArgs += argumentsAsString(_allImports, _argumentPrefix);
            }

            String throwArgs = getThrowArguments().isEmpty() ? "" : (" throws " + String.join(", ", getThrowArguments()));

            String assignments = "";

            if (!getSuperArguments().isEmpty()) {
                assignments = _indent + "super(" + getSuperArguments().stream().map(MemberOrArgument::getName).collect(Collectors.joining(", ")) + ");";
            }

            if (!getArguments().isEmpty()) {
                List<String> assigns = new ArrayList<>();
                for (MemberOrArgument e : getArguments()) {
                    assigns.add(_indent + "this." + e.getName().replaceFirst("^_(.+)", "$1") + " = " + maybePrefix(e.getName(), _argumentPrefix) + ";");
                }
                assignments += String.join(System.lineSeparator(), assigns);
            }

            return CONSTRUCTOR_TEMPL.formatted(_className, constructorArgs, throwArgs, assignments)
                .lines().map(l -> _indent + l).toList();

        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [arguments=" + arguments + ", superArguments=" + superArguments
                    + ", throwArguments=" + throwArguments + "]";
        }

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
