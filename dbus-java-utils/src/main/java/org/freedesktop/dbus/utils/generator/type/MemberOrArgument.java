package org.freedesktop.dbus.utils.generator.type;

import org.freedesktop.dbus.utils.bin.IdentifierMangler;
import org.freedesktop.dbus.utils.generator.TypeConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pojo which represents a class member/field or argument.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-20
 */
public class MemberOrArgument implements ICodeGenerator {

    /** Name of member/field. */
    private final String       name;
    /** Type of member/field (e.g. String, int...). */
    private String       type;
    /** True to force this member to be final, false otherwise. */
    private final boolean      finalArg;
    /** List of classes/types or placeholders put into diamond operators to use as generics. */
    private final List<String> generics    = new ArrayList<>();
    /** List of annotations for this member. */
    private final List<AnnotationInfo> annotations = new ArrayList<>();

    private final ClassBuilderInfo classBuilderInfo;

    public MemberOrArgument(ClassBuilderInfo _bldr, String _name, String _type, boolean _finalMember) {
        // repair reserved words by adding 'Param' as appendix, and when start with _ too
        name = (IdentifierMangler.isReservedWord(_name) || IdentifierMangler.isReservedWord(_name.replaceFirst("^_(.+)", "$1"))) ? _name + "param" : _name;
        type = _type;
        finalArg = _finalMember;
        classBuilderInfo = _bldr;
    }

    public MemberOrArgument(ClassBuilderInfo _bldr, String _name, String _type) {
        this(_bldr, _name, _type, false);
    }

    public List<AnnotationInfo> getAnnotations() {
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

    @Override
    public ClassBuilderInfo getClassBuilderInfo() {
        return classBuilderInfo;
    }

    @Override
    public List<String> generate(int _indentLevel) {
        List<String> result = new ArrayList<>();
        String memberType = TypeConverter.getProperJavaClass(getType(), getClassBuilderInfo().getImports());

        if (!getGenerics().isEmpty()) {
            memberType += "<" + getGenerics().stream().map(c -> TypeConverter.convertJavaType(c, false)).collect(Collectors.joining(", ")) + ">";
        }

        GetterMethod getterMethod = new GetterMethod(getClassBuilderInfo(), getName(), memberType, this);

        getClassBuilderInfo().getMethods().add(getterMethod);
        if (!isFinalArg()) {
            getClassBuilderInfo().getMethods().add(new SetterMethod(getClassBuilderInfo(), getName(), memberType));
        }

        return result;
    }

    public String asOneLineString(boolean _includeAnnotations) {
        StringBuilder sb = new StringBuilder();

        if (isFinalArg()) {
            sb.append("final ");
        }

        if (_includeAnnotations && !getAnnotations().isEmpty()) {
            sb.append(String.join(" ", getAnnotations().stream().map(e -> e.getAnnotationString()).toList()))
                    .append(" ");
        }

        sb.append(getFullType(getClassBuilderInfo().getImports()));

        sb.append(" ");

        sb.append(ClassBuilderInfo.maybePrefix(getName(), getClassBuilderInfo().getArgumentPrefix()));

        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [name=" + name + ", type=" + type + ", finalArg=" + finalArg + ", generics="
                + generics + ", annotations=" + annotations + "]";
    }

}
