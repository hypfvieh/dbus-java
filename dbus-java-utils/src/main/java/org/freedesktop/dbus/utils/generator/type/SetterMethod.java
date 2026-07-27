package org.freedesktop.dbus.utils.generator.type;

import java.util.List;

public class SetterMethod extends ClassMethod {

    private static final String SETTER_METHOD_TEMPL = """
        %s%s %s(%s) {
        %s%s
        }
        """;
    private final int     indentLevel;
    private final boolean declarationOnly;

    /** Setter for a concrete class member (generates a method body {@code this.x = x;}). */
    public SetterMethod(ClassBuilderInfo _bldr, int _indentLevel, String _name, String _setterType) {
        this(_bldr, _indentLevel, _name, _setterType, false);
    }

    /**
     * @param _declarationOnly if {@code true} only a declaration (no body) is generated, e.g. for interface
     *                         property setters; the parameter name is then lower-cased for readability
     */
    public SetterMethod(ClassBuilderInfo _bldr, int _indentLevel, String _name, String _setterType, boolean _declarationOnly) {
        super(_bldr, _name, "void", "set", false);
        indentLevel = _indentLevel;
        declarationOnly = _declarationOnly;
        String argName = _declarationOnly ? _name.substring(0, 1).toLowerCase() + _name.substring(1) : _name;
        getArguments().add(new MemberOrArgument(_bldr, argName, _setterType));
    }

    @Override
    protected List<String> formatMethod(int _indentLvl, String _modifier, String _returnType, String _methodName, String _args) {
        if (declarationOnly || getArguments() == null || getArguments().isEmpty()) {
            return super.formatMethod(_indentLvl, _modifier, _returnType, _methodName, _args);
        }

        int indent = Math.max(indentLevel, _indentLvl);
        String content = String.format("this.%s = %s;", getArguments().getFirst().getName(), getArguments().getFirst().getName());

        return SETTER_METHOD_TEMPL.formatted("public ", _returnType, _methodName, _args,
            getIndent(indent), content)
        .lines().map(l -> getIndent(indent) + l).toList();
    }
}
