package org.freedesktop.dbus.utils.generator;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.utils.Util;

/**
 * Helper to create Java class/interface files with proper formatting.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-22
 */
public class ClassBuilderInfo {

    /** Set of reserved words in Java. */
    private static final Set<String> RESERVED = new HashSet<>();
    static {
        RESERVED.add("abstract");
        RESERVED.add("assert");
        RESERVED.add("boolean");
        RESERVED.add("break");
        RESERVED.add("byte");
        RESERVED.add("case");
        RESERVED.add("catch");
        RESERVED.add("char");
        RESERVED.add("class");
        RESERVED.add("const");
        RESERVED.add("continue");
        RESERVED.add("default");
        RESERVED.add("do");
        RESERVED.add("double");
        RESERVED.add("else");
        RESERVED.add("enum");
        RESERVED.add("extends");
        RESERVED.add("final");
        RESERVED.add("finally");
        RESERVED.add("float");
        RESERVED.add("for");
        RESERVED.add("goto");
        RESERVED.add("if");
        RESERVED.add("implements");
        RESERVED.add("import");
        RESERVED.add("instanceof");
        RESERVED.add("int");
        RESERVED.add("interface");
        RESERVED.add("long");
        RESERVED.add("native");
        RESERVED.add("new");
        RESERVED.add("null");
        RESERVED.add("package");
        RESERVED.add("private");
        RESERVED.add("protected");
        RESERVED.add("public");
        RESERVED.add("return");
        RESERVED.add("short");
        RESERVED.add("static");
        RESERVED.add("strictfp");
        RESERVED.add("super");
        RESERVED.add("switch");
        RESERVED.add("synchronized");
        RESERVED.add("this");
        RESERVED.add("throw");
        RESERVED.add("throws");
        RESERVED.add("transient");
        RESERVED.add("try");
        RESERVED.add("void");
        RESERVED.add("volatile");
        RESERVED.add("while");
    }

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
        return String.join(System.lineSeparator(), result);
    }

    /**
     * Create the Java source for the class information provided.
     *
     * @param _staticClass this is static inner class
     * @param _otherImports this class needs additional imports (e.g. due to inner class)
     * @return
     */
    private List<String> createClassFileContent(boolean _staticClass, Set<String> _otherImports) {
        List<String> content = new ArrayList<>();

        String classIndent = _staticClass ? "    " : "";
        String memberIndent = _staticClass ? "        " : "    ";

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
            content.add("");
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
            Set<String> imports = getImportsForType(getExtendClass());
            getImports().addAll(imports);
            allImports.addAll(imports);
            bgn += " extends " + getSimpleTypeClasses(getExtendClass());
        }
        if (!getImplementedInterfaces().isEmpty()) {
            bgn += " implements " + getImplementedInterfaces().stream().map(e -> getClassName(e)).collect(Collectors.joining(", "));
            // add classes import if implements-arguments are not a java.lang. classes
            getImports().addAll(getImplementedInterfaces().stream().filter(s -> !s.startsWith("java.lang.")).collect(Collectors.toList()));
        }

        bgn += " {";

        content.add(bgn);
        if (_staticClass) {
            content.add("");
        }

        // add member fields
        for (MemberOrArgument member : members) {
            if (!member.getAnnotations().isEmpty()) {
                content.addAll(member.getAnnotations().stream().map(l -> memberIndent + l).collect(Collectors.toList()));
            }
            content.add(memberIndent + "private " + member.asOneLineString(allImports, false) + ";");
        }

        if (!getConstructors().isEmpty()) {
            content.add("");
            for (ClassConstructor constructor : getConstructors()) {
                String outerIndent = _staticClass ? "        " : "    ";
                String cstr = outerIndent + "public " + getClassName() + "(";

                List<ClassBuilderInfo.MemberOrArgument> filteredSuperArguments = new ArrayList<>(constructor.getSuperArguments());
                filteredSuperArguments.removeIf(e -> constructor.getArguments().contains(e));
                if (!filteredSuperArguments.isEmpty()) {
                    cstr += filteredSuperArguments.stream().map(e -> e.asOneLineString(allImports, false)).collect(Collectors.joining(", "));
                    if (!constructor.getArguments().isEmpty()) {
                        cstr += ", ";
                    }
                }

                if (!constructor.getArguments().isEmpty()) {
                    cstr += constructor.argumentsAsString(allImports);
                }

                if (constructor.getThrowArguments().isEmpty()) {
                    cstr += ") {";
                } else {
                    cstr += ") throws " + String.join(", ", constructor.getThrowArguments()) + " {";
                }

                content.add(cstr);

                String innerIndent = _staticClass ? "            " : "        ";

                if (!constructor.getSuperArguments().isEmpty()) {
                    content.add(innerIndent + "super(" + constructor.getSuperArguments().stream().map(c -> c.getName()).collect(Collectors.joining(", ")) + ");");
                }

                if (!constructor.getArguments().isEmpty()) {
                    for (MemberOrArgument e : constructor.getArguments()) {
                        content.add(innerIndent + "this." + e.getName().replaceFirst("^_(.+)", "$1") + " = " + e.getName() + ";");
                    }
                }

                content.add(outerIndent + "}");
            }
        }

        content.add("");

        // add getter and setter
        for (MemberOrArgument member : members) {
            String memberType = TypeConverter.getProperJavaClass(member.getType(), allImports);

            if (!member.getGenerics().isEmpty()) {
                memberType += "<" + member.getGenerics().stream().map(c -> TypeConverter.getProperJavaClass(c, allImports)).collect(Collectors.joining(", ")) + ">";
            }

            String getterSetterName = Util.snakeToCamelCase(Util.upperCaseFirstChar(member.getName()));
            if (!member.isFinalArg()) {
                content.add(memberIndent + "public void set" + getterSetterName + "("
                        + memberType + " arg) {");
                content.add(memberIndent + "    " + member.getName() + " = arg;");
                content.add(memberIndent + "}");
            }
            content.add("");
            content.add(memberIndent + "public " + memberType + " get" + getterSetterName + "() {");
            content.add(memberIndent + "    return " + member.getName() + ";");
            content.add(memberIndent + "}");
        }

        content.add("");

        for (ClassMethod mth : getMethods()) {

            if (!mth.getAnnotations().isEmpty()) {
                content.addAll(mth.getAnnotations().stream().map(a -> memberIndent + a).collect(Collectors.toList()));
            }

            String clzMth = memberIndent + "public " + (mth.getReturnType() == null ? "void " : TypeConverter.getProperJavaClass(mth.getReturnType(), allImports) + " ");
            clzMth += mth.getName() + "(";
            if (!mth.getArguments().isEmpty()) {
                clzMth += mth.getArguments().stream().map(e -> e.asOneLineString(allImports, true))
                        .collect(Collectors.joining(", "));
            }
            clzMth += ");";
            content.add(clzMth);
        }
        content.add("");

        for (ClassBuilderInfo inner : getInnerClasses()) {
            content.addAll(inner.createClassFileContent(true, allImports));
            allImports.addAll(inner.getImports()); // collect additional imports which may have been added by inner class
        }

        content.add(classIndent + "}");

        // write imports to resulting content if this is not a inner class
        if (!_staticClass) {
            content.add(2, "");
            content.addAll(2, allImports.stream().filter(l -> !l.startsWith("java.lang.")).map(l -> "import " + l + ";").collect(Collectors.toList()));
        }

        return content;
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
        StringBuffer sb = new StringBuffer();
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

    }

    /**
     * Pojo which represents a class member/field or argument.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class MemberOrArgument {
        /** Name of member/field. */
        private final String       name;
        /** Type of member/field (e.g. String, int...). */
        private final String       type;
        /** True to force this member to be final, false otherwise. */
        private final boolean      finalArg;
        /** List of classes/types or placeholders put into diamond operators to use as generics. */
        private final List<String> generics    = new ArrayList<>();
        /** List of annotations for this member. */
        private final List<String> annotations = new ArrayList<>();

        public MemberOrArgument(String _name, String _type, boolean _finalMember) {
            // repair reserved words by adding 'Param' as appendix
            name = RESERVED.contains(_name) ? _name + "param" : _name;
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
                        .append(getGenerics().stream().map(c -> TypeConverter.getProperJavaClass(c, _allImports)).collect(Collectors.joining(", ")))
                        .append(">");
            }

            return sb.toString();
        }

        public String asOneLineString(Set<String> _allImports, boolean _includeAnnotations) {
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

            sb.append(getName());

            return sb.toString();
        }

    }

    /**
     * Pojo which represents a class constructor.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class ClassConstructor {
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

        public String argumentsAsString(Set<String> _allImports) {
            return getArguments().stream().map(a -> a.asOneLineString(_allImports, true)).collect(Collectors.joining(", "));
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
