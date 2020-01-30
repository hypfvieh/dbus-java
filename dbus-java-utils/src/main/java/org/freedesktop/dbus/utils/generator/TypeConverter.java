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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.Variant;

import com.github.hypfvieh.util.StringUtil;

/**
 * Helper to convert DBus types and java types.
 * 
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-22
 */
public class TypeConverter {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    static {
        CLASS_MAP.put("java.lang.CharSequence", "String");
        CLASS_MAP.put("java.util.List", "List");
        CLASS_MAP.put("java.util.Set", "Set");
        CLASS_MAP.put("java.util.Map", "Map");
        CLASS_MAP.put(Variant.class.getName(), "Variant<?>");
    }

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
                    String plainClazzName = match.substring(match.lastIndexOf(".") +1);
                    clazzName = clazzName.replace(match, plainClazzName);
                }
            }
            
        } else {
            clazzName = _argType.substring(_argType.lastIndexOf(".") + 1);
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
    private static String convertJavaType(String _fqcn, boolean _usePrimitives) {
    	if (_fqcn == null) {
    		return _fqcn;
    	}
    	String clazzName = _fqcn;
    	
    	if (_fqcn.contains(".")) {
    		clazzName = _fqcn.substring(_fqcn.lastIndexOf(".") + 1);
    	}

        if (CLASS_MAP.containsKey(_fqcn)) {
            return CLASS_MAP.get(_fqcn);
        }

        if (clazzName.equals("CharSequence")) {
            return "String";
        }

        return _usePrimitives ? convertJavaBoxedTypeToPrimitive(clazzName) : clazzName;
    }

    /**
     * Converts certain boxed types to primitives.
     * 
     * @param _clazzName class name of boxed type
     * @return primitive or original input
     */
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
        
        if (StringUtil.isBlank(_dbusType)) {
        	return null;
        }
        
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

    /**
     * Resolve java type recursively.
     * 
     * @param _type Type object
     * @param _innerGenerics list which will be populated with classnames of the inner generic types (if any)
     * @return Map where key is parent classname (e.g. List) and value is a list of types used inside the generics
     * 
     * @throws DBusException on error
     */
    private static Map<String, List<String>> getTypeAdv(Type _type, List<String> _innerGenerics) throws DBusException {
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
    	
    	if (StringUtil.isBlank(_dbusType)) {
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
                    Map<String, List<String>> typeAdv = getTypeAdv(type, null);
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
