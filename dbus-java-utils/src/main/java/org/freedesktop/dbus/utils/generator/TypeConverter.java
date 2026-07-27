package org.freedesktop.dbus.utils.generator;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.*;
import org.freedesktop.dbus.utils.Util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper to convert DBus types and java types.
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-22
 */
public final class TypeConverter {

    private static final Map<String, String> CLASS_MAP = Map.of(
        "java.lang.CharSequence", "String",
        "java.util.List", "List",
        "java.util.Set", "Set",
        "java.util.Map", "Map",
        Variant.class.getName(), "Variant<?>");

    private static final Map<String, String> PRIMITIVE_TO_BOXED = Map.of(
        "byte", "Byte",
        "short", "Short",
        "int", "Integer",
        "long", "Long",
        "float", "Float",
        "double", "Double",
        "boolean", "Boolean",
        "char", "Character"
    );

    private static final Map<String, String> BOXED_TO_PRIMITIVE = Map.of(
        "Byte", "byte",
        "Short", "short",
        "Integer", "int",
        "Long", "long",
        "Float", "float",
        "Double", "double",
        "Boolean", "boolean",
        "Character", "char"
    );

    private TypeConverter() {}

    /**
     * Converts a java class type to another type.
     * This is used for converting e.g. CharSequence to String etc.
     * It will also remove unnecessary package names like java.lang.
     *
     * @param _argType Argument to convert
     * @param _includes Set where additional includes will be added (should never be null!)
     * @return String with proper type, if no converation could be done, original input is returned
     */
    public static String getProperJavaClass(String _argType, Set<String> _includes) {
        String clazzName = null;
        if (_argType == null) {
            return _argType;
        }

        // this is something with generics, so we do not convert boxed type to primitives
        if (_argType.contains("<")) {
            clazzName = _argType;
            for (Entry<String, String> clzzNames : CLASS_MAP.entrySet()) {
                if (clazzName.contains(clzzNames.getKey())) {
                    clazzName = clazzName.replace(clzzNames.getKey(), clzzNames.getValue());
                    if (!clzzNames.getKey().startsWith("java.lang.")) { // only add imports for classes not in java.lang (which is always in scope)
                        _includes.add(clzzNames.getKey());
                    }
                }
            }
            clazzName = clazzName.replace("java.lang.", "");

            Pattern compile = Pattern.compile("([^, <>]+)");
            Matcher matcher = compile.matcher(clazzName);
            while (matcher.find()) {
                String match = matcher.group();
                if (_includes.contains(match)) {
                    String plainClazzName = match.substring(match.lastIndexOf('.') + 1);
                    clazzName = clazzName.replace(match, plainClazzName);
                }
            }

        } else {
            clazzName = _argType.substring(_argType.lastIndexOf('.') + 1);
            // change some boxed types back to primitives
            return convertJavaType(clazzName, true);
        }
        return clazzName;
    }

    /**
     * Transform certain java types to other java types (see {@link #CLASS_MAP}).
     *
     * @param _fqcn fully qualified classname of the type to convert
     * @param _usePrimitives if true, boxed types will be converted to primitives
     * @return converted type or original input
     */
    public static String convertJavaType(String _fqcn, boolean _usePrimitives) {
        if (_fqcn == null) {
            return _fqcn;
        }
        String clazzName = _fqcn;

        if (_fqcn.contains(".")) {
            clazzName = _fqcn.substring(_fqcn.lastIndexOf('.') + 1);
        }

        if (CLASS_MAP.containsKey(_fqcn)) {
            return CLASS_MAP.get(_fqcn);
        }

        if ("CharSequence".equals(clazzName)) {
            return "String";
        } else if ("Variant".equals(clazzName)) {
            return "Variant<?>";
        }

        return _usePrimitives ? convertJavaBoxedTypeToPrimitive(clazzName) : clazzName;
    }

    /**
     * Converts certain boxed types to primitives.
     *
     * @param _clazzName class name of boxed type
     * @return primitive or original input
     */
    public static String convertJavaBoxedTypeToPrimitive(String _clazzName) {
        return  BOXED_TO_PRIMITIVE.getOrDefault(_clazzName, _clazzName);
    }

    /**
     * Converts certain primitives to boxed types.
     *
     * @param _primitiveName class name of primitve type
     * @return boxed type or original input
     */
    public static String convertJavaPrimitiveToBoxed(String _primitiveName) {
        return  PRIMITIVE_TO_BOXED.getOrDefault(_primitiveName, _primitiveName);
    }

    /**
     * Checks if the given class name is a primitive type.
     * @param _clazzName
     * @return true if primitive, false otherwise
     */
    public static boolean isPrimitive(String _clazzName) {
        return PRIMITIVE_TO_BOXED.containsKey(_clazzName);
    }

    /**
     * Converts a DBus data type string to java classname(s).
     *
     * @param _dbusType DBus data type string
     * @param _javaIncludes List where additional imports will be added to (should not be null!)
     * @return Java classname, maybe null if no suitable input was given
     *
     * @throws DBusException on DBus error
     */
    public static String getJavaTypeFromDBusType(String _dbusType, Set<String> _javaIncludes) throws DBusException {
        List<Type> dataType = new ArrayList<>();
        String type;

        if (Util.isBlank(_dbusType)) {
            return null;
        }

        if (_dbusType.length() == 1) {
            Marshalling.getJavaType(_dbusType, dataType, 1);

            type = dataType.stream()
                    .map(Type::getTypeName)
                    .collect(Collectors.joining(""));

            _javaIncludes.add(type);
        } else {
            type = getTypeAdv(_dbusType, _javaIncludes);
        }

        return type;
    }

    /**
     * Recursively builds the (possibly nested) java type string for the given {@link Type} and collects every
     * encountered type name (raw container types and leaf types) into {@code _javaIncludes} so the required
     * imports can be emitted.
     *
     * @param _type type to resolve
     * @param _javaIncludes set collecting the encountered type names (imports)
     * @return java type string, e.g. {@code java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>}
     */
    private static String buildJavaType(Type _type, Set<String> _javaIncludes) {
        if (_type instanceof ParameterizedType pType) {
            String raw = pType.getRawType().getTypeName();
            _javaIncludes.add(raw);
            String args = Arrays.stream(pType.getActualTypeArguments())
                .map(t -> buildJavaType(t, _javaIncludes))
                .collect(Collectors.joining(", "));
            return raw + "<" + args + ">";
        }

        String name = _type.getTypeName();
        _javaIncludes.add(name);
        return name;
    }

    /**
     * Special handling for {@link DBusMapType} and {@link DBusListType}. Produces the fully nested generic type
     * string (arbitrary depth, distinct map key/value types) via {@link #buildJavaType(Type, Set)}.
     *
     * @param _dbusType DBus type string
     * @param _javaIncludes list where additional java imports are added to (if any)
     * @return class name of the parent type, maybe null if no suitable input provided
     *
     * @throws DBusException on DBus error
     */
    private static String getTypeAdv(String _dbusType, Set<String> _javaIncludes) throws DBusException {

        if (Util.isBlank(_dbusType)) {
            return null;
        }

        List<Type> dataType = new ArrayList<>();
        Marshalling.getJavaType(_dbusType, dataType, 1);

        Type first = dataType.getFirst();
        if (first instanceof DBusListType || first instanceof DBusMapType) {
            return buildJavaType(first, _javaIncludes);
        }

        return first.getTypeName();
    }
}
