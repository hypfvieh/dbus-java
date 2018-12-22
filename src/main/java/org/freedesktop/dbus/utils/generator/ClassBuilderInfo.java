package org.freedesktop.dbus.utils.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.hypfvieh.util.StringUtil;

/**
 * Helper to create Java class/interface files with proper formatting.
 * 
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-22
 */
public class ClassBuilderInfo {
	/** Imported files for this class. */
    private final Set<String>            imports               = new LinkedHashSet<>();
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
        List<String> result = createClassFileContent(false, null);
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

        if (!_staticClass) {
            content.add("package " + getPackageName() + ";");
            content.add("");
            content.add("/**");
            content.add(" * Auto-generated class.");
            content.add(" */");

        } else {
            content.add("");
            if (_otherImports != null) {
                _otherImports.addAll(getImports());
            }
        }

        String bgn = classIndent + "public " + (_staticClass ? "static " : "") + (getClassType() == ClassType.INTERFACE ? "interface" : "class");
        bgn += " " + getClassName();
        if (getExtendClass() != null) {
            if (!getExtendClass().startsWith("java.lang.")) {
                getImports().add(getExtendClass()); // add class import if extends-argument is not a java.lang. class
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
            String memberType = TypeConverter.getProperJavaClass(member.getType(), _otherImports);
            if (!member.getGenerics().isEmpty()) {
                memberType += "<" + member.getGenerics().stream().map(c -> TypeConverter.getProperJavaClass(c, _otherImports)).collect(Collectors.joining(" ,")) + ">";
            }
            content.add(memberIndent + "private " + (member.isFinalMember() ? "final " : "") + memberType + " "
                    + member.getName() + ";");
        }

        if (!getConstructors().isEmpty()) {
            for (ClassConstructor constructor : getConstructors()) {
                String cstr = "    " + getClassName() + "(";
                if (!constructor.getArguments().isEmpty()) {
                    cstr += constructor.getArguments().entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
                }
                cstr += ") {";
                content.add(cstr);
                if (!constructor.getSuperArguments().isEmpty()) {
                    content.add("super(" + String.join(", ", constructor.getSuperArguments()) + ");");
                }
                if (!constructor.getArguments().isEmpty()) {
                    for (Entry<String, String> e : constructor.getArguments().entrySet()) {
                        content.add("this." + e.getKey() + " = " + e.getKey() + ";");
                    }
                }

                content.add("}");
            }
        }

        content.add("");

        // add getter and setter
        for (ClassMember member : members) {
            String memberType = TypeConverter.getProperJavaClass(member.getType(), _otherImports);

            if (!member.getGenerics().isEmpty()) {
                memberType += "<" + member.getGenerics().stream().map(c -> TypeConverter.getProperJavaClass(c, _otherImports)).collect(Collectors.joining(" ,")) + ">";
            }

            if (!member.isFinalMember()) {
                content.add(memberIndent + "public void set" + StringUtil.upperCaseFirstChar(member.getName()) + "("
                        + memberType + " arg) {");
                content.add(memberIndent + "    " + member.getName() + " = arg;");
                content.add(memberIndent +"}");
            }
            content.add("");
            content.add(memberIndent + "public " + memberType + " get" + StringUtil.upperCaseFirstChar(member.getName()) + "() {");
            content.add(memberIndent + "    return " + member.getName() + ";");
            content.add(memberIndent + "}");
        }

        content.add("");

        for (ClassMethod mth : getMethods()) {
        	
        	if (!mth.getAnnotations().isEmpty()) {
        		content.addAll(mth.getAnnotations().stream().map(a -> memberIndent + a).collect(Collectors.toList()));
        	}
        	
            String clzMth = memberIndent + "public " + (mth.getReturnType() == null ? "void " : TypeConverter.getProperJavaClass(mth.getReturnType(), _otherImports) + " ");
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
            content.addAll(inner.createClassFileContent(true, this.getImports()));
        }

        content.add(classIndent + "}");

        if (!_staticClass) {
            content.add(2,"");
            content.addAll(2, getImports().stream().filter(l -> !l.startsWith("java.lang.")).map(l -> "import " + l + ";").collect(Collectors.toList()));

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
            name = _name;
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
        private final List<String> superArguments = new ArrayList<>();
        
        public Map<String, String> getArguments() {
            return arguments;
        }
        public List<String> getSuperArguments() {
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
