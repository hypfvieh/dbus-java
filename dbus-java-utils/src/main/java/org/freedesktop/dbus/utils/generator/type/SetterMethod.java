package org.freedesktop.dbus.utils.generator.type;

import java.util.List;

public class SetterMethod extends ClassMethod {

    private static final String SETTER_METHOD_TEMPL = """
        %s%s %s(%s) {
        %s%s
        }
        """;

    public SetterMethod(ClassBuilderInfo _bldr, String _name, String _setterType) {
        super(_bldr, _name, "void", "set", false);
        getArguments().add(new MemberOrArgument(_bldr, _name, _setterType));
    }

    @Override
    protected List<String> formatMethod(int _indentLvl, String _modifier, String _returnType, String _methodName, String _args) {

        if (getArguments() == null || getArguments().isEmpty()) {
            return super.formatMethod(_indentLvl, _modifier, _returnType, _methodName, _args);
        }

        String content = String.format("this.%s = %s;", getArguments().getFirst().getName(), getArguments().getFirst().getName());

        return SETTER_METHOD_TEMPL.formatted(_modifier, _returnType, _methodName, _args,
            getIndent(Math.min(1, _indentLvl - 1)), content)
        .lines().map(l -> getIndent(_indentLvl) + l).toList();
    }
}
