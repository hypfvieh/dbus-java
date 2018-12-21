package org.freedesktop.dbus.utils.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.Variant;

public class TypeConverter {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    static {
        CLASS_MAP.put("java.lang.CharSequence", "String");
        CLASS_MAP.put("java.util.List", "List");
        CLASS_MAP.put("java.util.Set", "Set");
        CLASS_MAP.put("java.util.Map", "Map");
        CLASS_MAP.put(Variant.class.getName(), "Variant<?>");
    }


    public static String getProperJavaClass(String _argType, Set<String> _includes) {
        String clazzName = null;

        if (_argType.contains("<")) { // TODO: This is a class using generics
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

        } else {
            clazzName = _argType.substring(_argType.lastIndexOf(".") + 1);
            // change some boxed types back to primitives
            return convertJavaType(clazzName, true);
        }
        return clazzName;
    }

    private static String convertJavaType(String _fqcn, boolean _usePrimitives) {
        String clazzName = _fqcn.substring(_fqcn.lastIndexOf(".") + 1);

        if (CLASS_MAP.containsKey(_fqcn)) {
            return CLASS_MAP.get(_fqcn);
        }

        if (clazzName.equals("CharSequence")) {
            return "String";
        }

        return _usePrimitives ? convertJavaBoxedTypeToPrimitive(clazzName) : clazzName;
    }

    private static String convertJavaBoxedTypeToPrimitive(String _clazzName) {
        switch (_clazzName) {
            case "Boolean":
                return "boolean";
            case "Integer":
                return "int";
            case "Long":
                return "long";
            case "Double":
                return "double";
            case "Float":
                return "float";
            case "Byte":
                return "byte";
            case "Char":
                return "char";
            default:
                return _clazzName;
        }
    }

    public static String getJavaTypeFromDBusType(String _dbusType, Set<String> _javaIncludes) throws DBusException {
        List<Type> dataType = new ArrayList<>();
        String type;
        if (_dbusType.length() == 1) {
            Marshalling.getJavaType(_dbusType, dataType, 1);

             type = dataType.stream()
                    .map(t -> {
                        return t.getTypeName();
                    })
                    .collect(Collectors.joining(""));

             _javaIncludes.add(type);
        } else {
            type = getTypeAdv(_dbusType, _javaIncludes);
        }

        return type;
    }

    private static Map<String, List<String>> getTypeAdv(Type _type, List<String> _parts) throws DBusException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (_type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) _type;

            List<String> generics = new ArrayList<>();
            result.put(pType.getRawType().getTypeName(), generics);

            for (Type t : pType.getActualTypeArguments()) {
                if (t instanceof ParameterizedType) {
                    result.putAll(getTypeAdv(t, generics));
                } else {
                    generics.add(t.getTypeName());
                }
            }
        } else {
            result.put(_type.getTypeName(), new ArrayList<>());
        }
        return result;
    }

    private static String getTypeAdv(String _dbusType, Set<String> _javaIncludes) throws DBusException {
        List<Type> dataType = new ArrayList<>();
        Marshalling.getJavaType(_dbusType, dataType, 1);

        if (dataType.get(0) instanceof DBusListType || dataType.get(0) instanceof DBusMapType) {
            ParameterizedType dBusListType = (ParameterizedType) dataType.get(0);
            Type[] actualTypeArguments = dBusListType.getActualTypeArguments();

            String actualArgTypeVal = "?";

            String retVal = dBusListType.getRawType().getTypeName();

            if (actualTypeArguments.length > 0) {
                Map<String, List<String>> typeAdv = getTypeAdv(actualTypeArguments[0], null);

                actualArgTypeVal = "";
                for (Entry<String, List<String>> e : typeAdv.entrySet()) {
                    if (!e.getValue().isEmpty()) {
                        actualArgTypeVal += e.getKey() + "<";
                        actualArgTypeVal += String.join(", ", e.getValue());
                        actualArgTypeVal += ">";
                        _javaIncludes.addAll(e.getValue());
                    } else {
                        actualArgTypeVal = e.getKey();
                    }
                }
            }

            return retVal + "<" + actualArgTypeVal + ">";
        }

//        if (dataType.get(0) instanceof DBusStructType) {
//            DBusStructType dbusStructType = (DBusStructType) dataType.get(0);
//            Type rawType = dbusStructType.getRawType();
//            String retVal = rawType.getTypeName();
//
//            return retVal;
//        }

        return dataType.get(0).getTypeName();

    }
}
