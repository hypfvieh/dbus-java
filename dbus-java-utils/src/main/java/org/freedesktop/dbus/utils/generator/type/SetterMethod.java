package org.freedesktop.dbus.utils.generator.type;

import java.util.List;

public class SetterMethod extends ClassMethod {

    private static final String SETTER_METHOD_TEMPL = """
        %s%s %s(%s) {
        %s%s
        }
        """;
    private final int indentLevel;

    public SetterMethod(ClassBuilderInfo _bldr, int _indentLevel, String _name, String _setterType) {
        super(_bldr, _name, "void", "set", false);
        indentLevel = _indentLevel;
        getArguments().add(new MemberOrArgument(_bldr, _name, _setterType));
    }

    @Override
    protected List<String> formatMethod(int _indentLvl, String _modifier, String _returnType, String _methodName, String _args) {
        int indent = Math.max(indentLevel, _indentLvl);

        if (getArguments() == null || getArguments().isEmpty()) {
            return super.formatMethod(indent, _modifier, _returnType, _methodName, _args);
        }

        String content = String.format("this.%s = %s;", getArguments().getFirst().getName(), getArguments().getFirst().getName());

        return SETTER_METHOD_TEMPL.formatted("public ", _returnType, _methodName, _args,
            getIndent(indent), content)
        .lines().map(l -> getIndent(indent) + l).toList();
    }
}
