package org.freedesktop.dbus.utils.generator.type;

import java.util.List;

public interface ICodeGenerator {

    String INDENT = "    ";

    ClassBuilderInfo getClassBuilderInfo();

    List<String> generate(int _indentLevel);

    default String getIndent(int _indentLevel) {
        return INDENT.repeat(_indentLevel);
    }
}
