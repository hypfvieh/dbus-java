package org.freedesktop.dbus.utils.generator;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.freedesktop.dbus.annotations.DBusInterfaceName;

import com.github.hypfvieh.util.StringUtil;

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
    /** Members/Fields of this class. */
    private final List<ClassMember>      members               = new ArrayList<>();
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
    private ClassType                     classType;
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

    public List<ClassMember> getMembers() {
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
        
        String bgn = classIndent + "public " + (_staticClass ? "static " : "") + (getClassType() == ClassType.INTERFACE ? "interface" : "class");
        bgn += " " + getClassName();
        if (getExtendClass() != null) {
            if (!getExtendClass().startsWith("java.lang.")) {
                getImports().add(getExtendClass()); // add class import if extends-argument is not a java.lang. class
                allImports.add(getExtendClass());
            }

            bgn += " extends " + getClassName(getExtendClass());
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
        for (ClassMember member : members) {
            if (!member.getAnnotations().isEmpty()) {
                content.addAll(member.getAnnotations().stream().map(l -> memberIndent + l).collect(Collectors.toList()));
            }
            String memberType = TypeConverter.getProperJavaClass(member.getType(), allImports);
            if (!member.getGenerics().isEmpty()) {
                memberType += "<" + member.getGenerics().stream().map(c -> TypeConverter.getProperJavaClass(c, allImports)).collect(Collectors.joining(" ,")) + ">";
            }
            content.add(memberIndent + "private " + (member.isFinalMember() ? "final " : "") + memberType + " "
                    + member.getName() + ";");
        
        }

        if (!getConstructors().isEmpty()) {
            content.add("");
            for (ClassConstructor constructor : getConstructors()) {
                String outerIndent = _staticClass ? "        " : "    ";
                String cstr = outerIndent + getClassName() + "(";
                if (!constructor.getSuperArguments().isEmpty()) {
                    cstr += constructor.getSuperArguments().entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
                    if (!constructor.getArguments().isEmpty()) {
                        cstr += ", ";
                    }
                }
                if (!constructor.getArguments().isEmpty()) {
                    cstr += constructor.getArguments().entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
                }
                
                if (constructor.getThrowArguments().isEmpty()) {
                    cstr += ") {";
                } else {
                    cstr += ") throws " + String.join(", ", constructor.getThrowArguments()) + " {";
                }
                
                content.add(cstr);
                
                String innerIndent = _staticClass ? "            " : "        ";
                
                if (!constructor.getSuperArguments().isEmpty()) {
                    content.add(innerIndent + "super(" + String.join(", ", constructor.getSuperArguments().keySet()) + ");");
                }
                if (!constructor.getArguments().isEmpty()) {
                    for (Entry<String, String> e : constructor.getArguments().entrySet()) {
                        content.add(innerIndent + "this." + e.getKey().replaceFirst("^_(.+)", "$1") + " = " + e.getKey() + ";");
                    }
                }

                content.add(outerIndent + "}");
            }
        }

        content.add("");

        // add getter and setter
        for (ClassMember member : members) {
            String memberType = TypeConverter.getProperJavaClass(member.getType(), allImports);

            if (!member.getGenerics().isEmpty()) {
                memberType += "<" + member.getGenerics().stream().map(c -> TypeConverter.getProperJavaClass(c, allImports)).collect(Collectors.joining(" ,")) + ">";
            }

            String getterSetterName = StringUtil.snakeToCamelCase(StringUtil.upperCaseFirstChar(member.getName()));
            if (!member.isFinalMember()) {
                content.add(memberIndent + "public void set" + getterSetterName + "("
                        + memberType + " arg) {");
                content.add(memberIndent + "    " + member.getName() + " = arg;");
                content.add(memberIndent +"}");
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
                clzMth += mth.getArguments().entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
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
            content.add(2,"");
            content.addAll(2, allImports.stream().filter(l -> !l.startsWith("java.lang.")).map(l -> "import " + l + ";").collect(Collectors.toList()));
        }

        return content;
    }

    /**
     * Create the filename with path this java class should use.
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
     * @return String
     */
    public String getFqcn() {
        return StringUtil.isBlank(getPackageName()) ? getClassName() : getPackageName() + "." + getClassName();
    }

    /**
     * Extract the class name from a given FQCN (fully qualified classname).
     * @param _fqcn fqcn to analyze
     * @return classname, null if input was null
     */
    static String getClassName(String _fqcn) {
        if (_fqcn == null) {
            return null;
        }

        String clzzName = _fqcn;
        if (clzzName.contains(".")) {
            clzzName = clzzName.substring(clzzName.lastIndexOf(".") + 1);
        }
        return clzzName;
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
        private final String annotationParams;
        
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
        private final String              name;
        /** Return value of the method. */
        private final String              returnType;
        /** True if method should be final, false otherwise. */
        private final boolean             finalMethod;
        /** Arguments for this method, key is argument name, value is argument type. */
        private final Map<String, String> arguments = new LinkedHashMap<>();
        /** List of annotations for this method. */
        private final List<String> annotations = new ArrayList<>();
        
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

        public Map<String, String> getArguments() {
            return arguments;
        }

		public List<String> getAnnotations() {
			return annotations;
		}
        
    }

    /**
     * Pojo which represents a class member/field.
     * 
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class ClassMember {
    	/** Name of member/field. */
        private final String       name;
        /** Type of member/field (e.g. String, int...). */
        private final String       type;
        /** True to force this member to be final, false otherwise. */
        private final boolean      finalMember;
        /** List of classes/types or placeholders put into diamond operators to use as generics. */
        private final List<String> generics = new ArrayList<>();
        /** List of annotations for this member. */
        private final List<String> annotations = new ArrayList<>();

        public ClassMember(String _name, String _type, boolean _finalMember) {
            // repair reserved words by adding 'Param' as appendix
        	name = RESERVED.contains(_name) ? _name + "Param" : _name;
            type = _type;
            finalMember = _finalMember;
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

        public boolean isFinalMember() {
            return finalMember;
        }

        public List<String> getGenerics() {
            return generics;
        }

    }

    /**
     * Pojo which represents a class constructor.
     * 
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static class ClassConstructor {
    	/** List of arguments for the constructor. Key is argument name, value is argument type. */
        private final Map<String, String> arguments = new LinkedHashMap<>();
        /** List of arguments for the super-constructor. Key is argument name, value is argument type. */
        private final Map<String, String> superArguments = new LinkedHashMap<>();
        
        /** List of throws arguments. */
        private final List<String> throwArguments = new ArrayList<>();
        
        public List<String> getThrowArguments() {
            return throwArguments;
        }
        
        public Map<String, String> getArguments() {
            return arguments;
        }
        
        public Map<String, String> getSuperArguments() {
            return superArguments;
        }
    }

    /**
     * Enum to define either the {@link ClassBuilderInfo} is for a CLASS or an INTERFACE.
     * 
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-20
     */
    public static enum ClassType {
        INTERFACE,
        CLASS;
    }

}
