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
     * Resolve java type recursively.
     *
     * @param _type Type object
     * @return Map where key is parent classname (e.g. List) and value is a list of types used inside the generics
     *
     * @throws DBusException on error
     */
    private static Map<String, List<String>> getTypeAdv(Type _type) throws DBusException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (_type instanceof ParameterizedType pType) {

            List<String> generics = new ArrayList<>();
            result.put(pType.getRawType().getTypeName(), generics);

            for (Type t : pType.getActualTypeArguments()) {
                if (t instanceof ParameterizedType) {
                    result.putAll(getTypeAdv(t));
                } else {
                    generics.add(t.getTypeName());
                }
            }
        } else {
            result.put(_type.getTypeName(), new ArrayList<>());
        }
        return result;
    }

    /**
     * Special handling for {@link DBusMapType} and {@link DBusListType}.
     *
     * @param _dbusType DBus type string
     * @param _javaIncludes list where additional java imports are added to (if any)
     * @return class name of the parent type, maybe null if no suitable input provided
     *
     * @throws DBusException
     */
    private static String getTypeAdv(String _dbusType, Set<String> _javaIncludes) throws DBusException {

        if (Util.isBlank(_dbusType)) {
            return null;
        }

        List<Type> dataType = new ArrayList<>();
        Marshalling.getJavaType(_dbusType, dataType, 1);

        if (dataType.get(0) instanceof DBusListType || dataType.get(0) instanceof DBusMapType) {
            ParameterizedType dBusListType = (ParameterizedType) dataType.get(0);
            Type[] actualTypeArguments = dBusListType.getActualTypeArguments();

            String retVal = dBusListType.getRawType().getTypeName();
            List<String> internalTypes = new ArrayList<>();

            if (actualTypeArguments.length > 0) {
                Map<String, List<String>> allAdvTypes = new LinkedHashMap<>();

                for (Type type : actualTypeArguments) {
                    Map<String, List<String>> typeAdv = getTypeAdv(type);
                    allAdvTypes.putAll(typeAdv);
                }

                for (Entry<String, List<String>> e : allAdvTypes.entrySet()) {
                    if (!e.getValue().isEmpty()) {
                        String actualArgTypeVal = e.getKey() + "<";
                        actualArgTypeVal += String.join(", ", e.getValue());
                        actualArgTypeVal += ">";
                        internalTypes.add(actualArgTypeVal);
                        _javaIncludes.addAll(e.getValue());
                    } else {
                        internalTypes.add(e.getKey());
                    }
                }
            }

            // if key and value of map is of same type:
            if (dataType.get(0) instanceof DBusMapType && internalTypes.size() == 1) {
                internalTypes.add(internalTypes.get(0));
            }

            return retVal + "<" + String.join(", ", internalTypes) + ">";
        }

        return dataType.get(0).getTypeName();
    }
}
