package org.freedesktop.dbus.utils.generator.type;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.utils.Util;
import org.freedesktop.dbus.utils.generator.TypeConverter;
import org.freedesktop.dbus.utils.generator.type.AnnotationInfo.AnnotArgs;
import org.freedesktop.dbus.utils.generator.type.ClassBuilderInfo.ClassType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pojo which represents a class method.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-20
 */
public class ClassMethod implements ICodeGenerator {

    private static final String METHOD_TEMPL = """
        %s%s %s(%s);
        """;

    /** Name of this method. */
    private final String                 name;
    /** Return value of the method. */
    private final String                 returnType;
    /** Prefix for method name (e.g. get or is), {@code null} if not needed. */
    private final String                 methodPrefix;
    /** True if method should be final, false otherwise. */
    private final boolean                finalMethod;
    /** Arguments for this method, key is argument name, value is argument type. */
    private final List<MemberOrArgument> arguments    = new ArrayList<>();
    /** List of annotations for this method. */
    private final List<AnnotationInfo>   annotations  = new ArrayList<>();

    private final ClassBuilderInfo classBuilderInfo;

    public ClassMethod(ClassBuilderInfo _bldr, String _name, String _returnType, boolean _finalMethod) {
        this(_bldr, _name, _returnType, null, _finalMethod);
    }

    public ClassMethod(ClassBuilderInfo _bldr, String _name, String _returnType, String _methodPrefix, boolean _finalMethod) {
        name = _name;
        returnType = _returnType;
        finalMethod = _finalMethod;
        methodPrefix = _methodPrefix;
        classBuilderInfo = _bldr;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getMethodPrefix() {
        return methodPrefix;
    }

    public boolean isFinalMethod() {
        return finalMethod;
    }

    public List<MemberOrArgument> getArguments() {
        return arguments;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    @Override
    public ClassBuilderInfo getClassBuilderInfo() {
        return classBuilderInfo;
    }

    public String getMethodName() {
        boolean hasPrefix = !Util.isBlank(getMethodPrefix());
        String methodName = reformatName();

        if (hasPrefix) {
            methodName = getMethodPrefix() + Util.upperCaseFirstChar(methodName);
        }

        if (ClassBuilderInfo.isReservedMethodName(methodName)) {
            methodName += "FromBus";
        }

        return methodName;
    }

    private String reformatName() {
        return Util.kebabToCamelCase(Util.snakeToCamelCase(getName()));
    }

    @Override
    public List<String> generate(int _indentLevel) {
        List<String> result = new ArrayList<>();

        List<AnnotationInfo> currentAnnotations = new ArrayList<>(getAnnotations());
        String methodName = getMethodName();

        // java method name differs from bus method name -> add annotation to mitigate
        if (!getName().equals(methodName)) {
            if (currentAnnotations.stream().anyMatch(e -> e.getAnnotationClass() == DBusBoundProperty.class)) {

                // add "name" definition if original name differs from reformatted name
                // e.g. some-name != someName
                String reformattedMethodName = reformatName();
                if (!reformattedMethodName.equals(getName())
                    || !Util.upperCaseFirstChar(getName()).equals(reformattedMethodName)) {
                    currentAnnotations.stream().filter(e -> e.getAnnotationClass() == DBusBoundProperty.class)
                    .forEach(e -> e.getAnnotationParams().put("name", getName()));

                }

            } else if (Util.isBlank(getMethodPrefix())) {
                currentAnnotations.add(new AnnotationInfo(DBusMemberName.class, AnnotArgs.create().add(getName())));
            }
        }

        if (!currentAnnotations.isEmpty()) {
            result.addAll(currentAnnotations.stream().map(a -> getIndent(_indentLevel) + a.getAnnotationString()).toList());
        }

        String publicModifier = getClassBuilderInfo().getClassType() != ClassType.INTERFACE ? "public " : "";
        String mthReturnType = getReturnType() == null ? "void"
            : TypeConverter.getProperJavaClass(getReturnType(), getClassBuilderInfo().getImports());
        String args = "";

        if (!getArguments().isEmpty()) {
            args += getArguments().stream()
                .map(e -> e.asOneLineString(true))
                    .collect(Collectors.joining(", "));
        }

        result.addAll(formatMethod(_indentLevel, publicModifier, mthReturnType, methodName, args));

        return result;
    }

    protected List<String> formatMethod(int _indentLvl, String _modifier, String _returnType, String _methodName, String _args) {
        return METHOD_TEMPL.formatted(_modifier, _returnType, _methodName, _args)
        .lines().map(l -> getIndent(_indentLvl) + l).toList();
    }

}
