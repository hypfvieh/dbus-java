package org.freedesktop.dbus.utils.generator.type;

import java.util.List;

public class GetterMethod extends ClassMethod {

    private static final String GETTER_METHOD_TEMPL = """
        %s%s %s(%s) {
        %s%s
        }
        """;
    private final MemberOrArgument argument;

    private final int indentLevel;

    public GetterMethod(ClassBuilderInfo _bldr, String _name, String _returnType) {
        this(_bldr, 0, _name, _returnType, null);
    }

    public GetterMethod(ClassBuilderInfo _bldr, int _indentLevel, String _name, String _returnType, MemberOrArgument _arguments) {
        super(_bldr, _name, _returnType, "boolean".equalsIgnoreCase(_returnType) ? "is" : "get", false);
        this.argument = _arguments;
        this.indentLevel = _indentLevel;
    }

    @Override
    protected List<String> formatMethod(int _indentLvl, String _modifier, String _returnType, String _methodName, String _args) {
        int indent = Math.max(_indentLvl, indentLevel);
        if (argument == null) {
            return super.formatMethod(indent, _modifier, _returnType, _methodName, _args);
        }

        String content = String.format("return %s;", argument.getName());

        return GETTER_METHOD_TEMPL.formatted("public ", _returnType, _methodName, _args,
            getIndent(indent), content)
        .lines().map(l -> getIndent(indent) + l).toList();
    }

}
