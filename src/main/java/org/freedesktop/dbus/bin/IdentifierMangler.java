/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import java.util.Arrays;

/**
 * Checks identifiers for keywords etc and mangles them if so.
 */
public class IdentifierMangler {
    private static String[] keywords;
    static {
        keywords = new String[] {
                "true", "false", "null", "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof",
                "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"
        };
        Arrays.sort(keywords);
    }

    public static String mangle(String name) {
        if (Arrays.binarySearch(keywords, name) >= 0) {
            name = "_" + name;
        }
        return name;
    }
}
