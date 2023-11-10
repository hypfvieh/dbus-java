package org.freedesktop.dbus.utils.translator;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.DBusStructType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a DBus signature string and converts it to java classes tree.<br>
 * Uses a DBus signature string like "a(ia{sv})" and converts it something like:
 *
 * <pre>
 * java.util.List
 *    org.freedesktop.dbus.Struct
 *          java.lang.Integer
 *          java.util.Map
 *                java.lang.String
 *                org.freedesktop.dbus.types.Variant
 * </pre>
 *
 * Each indent step represents another step in the hierarchy.<br>
 * Classes listed in the same indent level are part of the same structure (e.g. Map, Struct).
 * <p>
 * In the example above, the signature was translated to a List of Struct where the
 * Struct has an Integer and a Map member. The Map consists of a CharSequence/String as key and
 * a Variant as value.
 * <p>
 * Call the static main of this class with the signature which should be translated as first argument.
 *
 * @author hypfvieh
 * @since v3.3.0 - 2020-12-28
 */
public final class DBusTypeStringToJava {

    private static final String INDENT = "   ";

    private DBusTypeStringToJava() {}

    public static void main(String[] _args) throws DBusException {
        if (_args == null || _args.length < 1) {
            System.err.println("Signature [e.g. a(ia{sv})] as first argument required");
            return;
        }

        String sig = _args[0];
        System.out.println("Parsing DBus signature: " + sig);

        List<Type> dataType = new ArrayList<>();
        int readBytes = Marshalling.getJavaType(sig, dataType, -1);

        System.out.println(readBytes + " of " + sig.length() + " bytes read");
        System.out.println("Object-Tree:");
        System.out.println("=====================");
        System.out.println();

        for (Type t : dataType) {
            recursive(t, 1);
        }

        System.out.println();
        System.out.println("=====================");
        System.out.println("Done");

    }

    private static void recursive(Type _t, int _indent) {
        if (_t instanceof DBusListType l) {
            System.out.println(repeat(INDENT, _indent) + List.class.getName());
            Type type = l.getActualTypeArguments()[0];
            recursive(type, _indent + 1);
        } else if (_t instanceof DBusMapType m) {
            System.out.println(repeat(INDENT, _indent) + Map.class.getName());
            recursive(m.getActualTypeArguments()[0], _indent + 2);
            recursive(m.getActualTypeArguments()[1], _indent + 2);
        } else if (_t instanceof DBusStructType s) {
            System.out.println(repeat(INDENT, _indent) + Struct.class.getName());
            for (Type ty : s.getActualTypeArguments()) {
                recursive(ty, _indent + 2);
            }
        } else {
            String str = _t.getTypeName();
            if ("java.lang.CharSequence".equals(str)) {
                str = String.class.getName();
            }
            System.out.println(repeat(INDENT, _indent) + str);
        }
    }

    /**
     * Repeat the given String given count times.
     *
     * @param _string string to repeat
     * @param _cnt count
     * @return repeated string
     */
    private static String repeat(String _string, int _cnt) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < _cnt; i++) {
            sb.append(_string);
        }
        return sb.toString();
    }
}
