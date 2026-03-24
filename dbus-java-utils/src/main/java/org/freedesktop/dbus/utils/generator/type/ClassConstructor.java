package org.freedesktop.dbus.utils.generator.type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pojo which represents a class constructor.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-20
 */
public class ClassConstructor implements ICodeGenerator {
    private static final String          CONSTRUCTOR_TEMPL =
        """
        public %s(%s)%s {
        %s
        }
        """;

    /** Map of arguments for the constructor. Key is argument name, value is argument type. */
    private final List<MemberOrArgument> arguments      = new ArrayList<>();
    /** Map of arguments for the super-constructor. Key is argument name, value is argument type. */
    private final List<MemberOrArgument> superArguments = new ArrayList<>();

    /** List of throws arguments. */
    private final List<String>           throwArguments = new ArrayList<>();

    private final ClassBuilderInfo classBuilderInfo;

    private final String className;
    private final int indentLevel;

    public ClassConstructor(ClassBuilderInfo _bldr, int _indentLevel, String _className) {
        classBuilderInfo = _bldr;
        className = _className;
        indentLevel = _indentLevel;
    }

    public List<String> getThrowArguments() {
        return throwArguments;
    }

    public List<MemberOrArgument> getArguments() {
        return arguments;
    }

    public List<MemberOrArgument> getSuperArguments() {
        return superArguments;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public ClassBuilderInfo getClassBuilderInfo() {
        return classBuilderInfo;
    }

    @Override
    public List<String> generate(int _indentLevel) {
        List<MemberOrArgument> filteredSuperArguments = new ArrayList<>(getSuperArguments());
        filteredSuperArguments.removeIf(e -> getArguments().contains(e));
        String constructorArgs = "";

        if (!filteredSuperArguments.isEmpty()) {
            constructorArgs += filteredSuperArguments.stream().map(e -> e.asOneLineString(false)).collect(Collectors.joining(", "));
            if (!getArguments().isEmpty()) {
                constructorArgs += ", ";
            }
        }

        if (!getArguments().isEmpty()) {
            constructorArgs += argumentsAsString();
        }

        String throwArgs = getThrowArguments().isEmpty() ? "" : (" throws " + String.join(", ", getThrowArguments()));

        String assignments = "";

        String prefix = getClassBuilderInfo().getArgumentPrefix();

        if (!getSuperArguments().isEmpty()) {
            assignments = getIndent(_indentLevel) + "super(" + getSuperArguments().stream()
                .map(e -> ClassBuilderInfo.maybePrefix(e.getName(), prefix))
                .collect(Collectors.joining(", ")) + ");" + System.lineSeparator();
        }

        if (!getArguments().isEmpty()) {
            List<String> assigns = new ArrayList<>();
            String innerIndent = getIndent(_indentLevel);
            for (MemberOrArgument e : getArguments()) {
                assigns.add(innerIndent + "this." + e.getName() + " = " + ClassBuilderInfo.maybePrefix(e.getName(), prefix) + ";");
            }
            assignments += String.join(System.lineSeparator(), assigns);
        }

        return CONSTRUCTOR_TEMPL.formatted(getClassName(), constructorArgs, throwArgs, assignments)
            .lines().map(l -> getIndent(_indentLevel + indentLevel) + l).toList();
    }

    public String argumentsAsString() {
        return getArguments().stream().map(a -> a.asOneLineString(true)).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [arguments=" + arguments + ", superArguments=" + superArguments
                + ", throwArguments=" + throwArguments + "]";
    }

}
