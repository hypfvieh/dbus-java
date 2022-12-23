package org.freedesktop.dbus.utils.bin;

import java.util.HashSet;
import java.util.Set;

/**
 * Checks identifiers for keywords etc and mangles them if so.
 */
public final class IdentifierMangler {
    private static final Set<String> KEYWORDS = new HashSet<>();

    // list of keywords as of JDK 19.
    // see: https://docs.oracle.com/javase/specs/jls/se19/html/jls-3.html#jls-3.9
    static {
        KEYWORDS.addAll(Set.of(
                "abstract", "assert", "boolean", "break",
                "byte", "case", "catch", "char", "class",
                "const", "continue", "default", "do",
                "double", "else", "enum", "exports", "extends",
                "final", "finally", "float", "for",
                "goto", "if", "implements", "import",
                "instanceof", "int", "interface", "long",
                "module", "native", "new",
                "non-sealed", "open", "opens",
                "package", "permits", "private",
                "protected", "provides", "public", "record",
                "requires", "return", "sealed", "short",
                "static", "strictfp", "super",
                "switch", "synchronized", "this",
                "throw", "throws", "to", "transient",
                "transitive", "try", "uses", "var",
                "void", "volatile", "while", "with",
                "yield"
        ));
    }

    private IdentifierMangler() {

    }

    /**
     * Checks if given value is a reserved keyword in Java.
     *
     * @param _name name to check
     * @return true if reserved
     */
    public static boolean isReservedWord(String _name) {
        return KEYWORDS.contains(_name);
    }

    /**
     * Checks if given name is a reserved word in Java.
     * <p>
     * If true, returns the given name with a "_" prepended.
     * Otherwise returns the input string.
     * </p>
     *
     * @param _name name to check
     * @return String
     */
    public static String mangle(String _name) {
        if (KEYWORDS.contains(_name)) {
            return "_" + _name;
        }
        return _name;
    }
}
