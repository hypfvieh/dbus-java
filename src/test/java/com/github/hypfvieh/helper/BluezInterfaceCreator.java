package com.github.hypfvieh.helper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bluez.datatypes.ThreeTuple;
import org.bluez.datatypes.TwoTuple;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

import com.github.hypfvieh.util.FileIoUtil;
import com.github.hypfvieh.util.StringUtil;
import com.github.hypfvieh.util.SystemUtil;

/**
 * Utility to generate interface classes from bluez documentation (doc/ directory of bluez source).
 *
 * @author hypfvieh
 * @since v0.1.0 - 2018-03-07
 */
public class BluezInterfaceCreator {

    private static final Pattern METHOD_REGEX_3PARTS = Pattern.compile("([^A-Z]+)\\s*([^\\(]*)\\(([^\\)]*)\\)(?:.*)");
    private static final Pattern METHOD_REGEX_2PARTS = Pattern.compile("([A-Za-z]+)\\s*\\(([^\\)]*)\\)(?:.*)");

    public static void main(String[] _args) throws IOException {
        if (_args.length < 2) {
            System.out.println("Usage: " + BluezInterfaceCreator.class.getName() + " path-to-bluez-doc-directory output-directory");
            System.exit(1);
        }

        File docPath = new File(_args[0]);
        if (!docPath.exists()) {
            System.err.println("Cannot find given bluez documentation path: " + docPath);
            System.exit(1);
        }

        List<File> fileList = Files.walk(docPath.toPath())
            .map(p -> p.toFile())
//            .filter(f -> f.getName().contains("agent-api"))
            .filter(p -> SystemUtil.getFileExtension(p.getAbsolutePath()).equals("txt"))
            .collect(Collectors.toList());

        Set<InterfaceStructure> structure = new LinkedHashSet<>();

        for (File file : fileList) {
            System.out.println("-> Reading: " + file);

            List<String> fileContent = FileIoUtil.readFileToList(file);
            InterfaceStructure is = null;


            boolean objectPath = false;
            for (int i = 0; i < fileContent.size(); i++) {
                String line = fileContent.get(i);

                    if (line.startsWith("=")) {
                        is = new InterfaceStructure();
                        continue;
                    }

                    if (is != null) {

                        if (line.matches("^Interface[^a-zA-Z0-9]+.+")) {
                            String interfaceName = line.replaceAll(".+\\s+(.+)", "$1");
                            String[] split = interfaceName.split("\\.");

                            is.interfaceName = split[split.length-1];
                            is.packageName = String.join(".", Arrays.asList(split).subList(0, split.length-1));
                            is.bluezInterface = interfaceName;
                            is.bluezDocFile = file.getName();
                            continue;
                        } else if (line.startsWith("Service")) {
                            String serviceName = line.replaceAll("Service\\s*", "");
                            is.bluezService = serviceName;
                            continue;
                        } else if (line.startsWith("Object path")) {
                            is.bluezObjectPath.add(line.replaceAll("Object path\\s*", ""));
                            objectPath = true;
                        } else if (objectPath && !StringUtil.isBlank(line)) {
                            is.bluezObjectPath.add(line.trim());
                            continue;
                        } else if (StringUtil.isBlank(line)) {
                            objectPath = false;
                        }

                        if (is.interfaceName != null) {
                            i += createInterfaceMethod(is, fileContent.subList(i, fileContent.size()));
                            structure.add(is);
                        }
                    }
            }
            if (is == null || is.packageName == null || is.interfaceName == null) {
                continue;
            }

        }

        createInterfaceFiles(_args, structure);
    }

    private static void createInterfaceFiles(String[] _args, Set<InterfaceStructure> structure) throws IOException {
        String nl = System.lineSeparator();
        String indent = "    ";
        String today = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now());
        for (InterfaceStructure is : structure) {
            StringBuilder sb = new StringBuilder();

            Set<String> imports = new TreeSet<>();
            Set<String> types = new TreeSet<>();
            // package and imports
            sb.append("package ").append(is.packageName).append(";").append(nl);
            sb.append(nl);
            sb.append("import ").append(DBusInterface.class.getName()).append(";").append(nl);
            if (is.additionalInterfaces.contains("Properties")) {
                sb.append("import ").append(org.freedesktop.dbus.interfaces.Properties.class.getName()).append(";").append(nl);
            }
            sb.append("IMPORTS_PLACEHOLDER");

            // java doc on class
            sb.append("/**").append(nl);
            sb.append(" * ").append("File generated - ").append(today).append(".<br>").append(nl);
            sb.append(" * ").append("Based on bluez Documentation: ").append(is.bluezDocFile).append(".<br>").append(nl);
            sb.append(" * <br>").append(nl);
            if (is.bluezService != null) {
                sb.append(" * ").append("<b>Service:</b> ").append(is.bluezService).append("<br>").append(nl);
            }
            if (is.bluezInterface != null) {
                sb.append(" * ").append("<b>Interface:</b> ").append(is.bluezInterface).append("<br>").append(nl);
            }
            if (!is.bluezObjectPath.isEmpty()) {
                sb.append(" * <br>").append(nl);
                sb.append(" * <b>Object path:</b><br>").append(nl);
                for (String objPath : is.bluezObjectPath) {
                    sb.append(" * ").append(indent).append(indent).append(indent).append(objPath).append("<br>").append(nl);
                }
            }
            sb.append(" * <br>").append(nl);

            // add properties documentation
            if (!is.propertiesDoc.isEmpty()) {
                sb.append(" * <b>Supported properties:</b> <br>").append(nl);
                sb.append(" * <br>").append(nl);
                for (String docLine : is.propertiesDoc) {
                    sb.append(" * ").append(docLine).append("<br>").append(nl);
                }
                sb.append(" * <br>").append(nl);
            }
            sb.append(" */").append(nl);

            // interface class

            sb.append("public interface ").append(is.interfaceName).append(" extends ");
            sb.append(String.join(", ", is.additionalInterfaces));
            sb.append(" {").append(nl);

            sb.append(nl);
            for (InterfaceMethod im : is.methods) {
                sb.append(indent).append("/**").append(nl);


                // read method parameters so we can also use it for javadoc
                List<String> methodParameterList = new ArrayList<>();
                List<String> methodParametersWithoutType = new ArrayList<>();
                if (!im.methodParameters.isEmpty()) {
                    for (Map<String, String> para : im.methodParameters) {
                        if (!para.isEmpty()) {
                            Entry<String, String> next = para.entrySet().iterator().next();
                            types.add(next.getKey());
                            methodParameterList.add(next.getKey() + " " + next.getValue());
                            methodParametersWithoutType.add(next.getValue());
                        }
                    }
                }

                if (im.documentation.isEmpty()) {
                    sb.append(indent).append(" * (No documentation yet).");
                } else {
                    sb.append(indent).append(" * <b>From bluez documentation:</b><br>").append(nl);
                    for (String docLine : im.documentation) {
                        sb.append(indent).append(" * ").append(docLine).append("<br>").append(nl);
                    }
                }
                if (!methodParametersWithoutType.isEmpty()) {
                    sb.append(indent).append(" * ").append(nl);
                    for (String string : methodParametersWithoutType) {
                        sb.append(indent).append(" * ").append("@param ").append(string).append(nl);
                    }
                }
                if (!im.exceptions.isEmpty()) {
                    sb.append(indent).append(" * ").append(nl);
                    for (String ex : im.exceptions) {
                        String[] split = ex.split("\\.");
                        sb.append(indent).append(" * ").append("@throws ").append(split[split.length-1]).append(nl);
                    }
                }

                sb.append(indent).append(" */").append(nl);
                types.add(im.returnType);
                if (im.deprecated) {
                    sb.append(indent).append("@Deprecated").append(nl);
                }
                sb.append(indent).append(im.returnType).append(" ").append(im.methodName).append("(");

                // add method parameters
                sb.append(String.join(", ", methodParameterList));

                sb.append(")");
                if (!im.exceptions.isEmpty()) {
                    sb.append(" throws ");
                    imports.addAll(im.exceptions);
                    sb.append(String.join(", ", im.exceptions).replace("org.bluez.exceptions.", ""));
                }
                sb.append(";").append(nl).append(nl);
            }

            if (!is.signals.isEmpty()) {
                createSignals(sb, is, indent, nl);
                // if signals are available, we need additional imports
                imports.add(DBusSignal.class.getName());
                imports.add(DBusException.class.getName());
            }

            sb.append("}").append(nl);

            String result = sb.toString();

            for (String type : types) {
                if (type.contains("Variant<")) {
                    imports.add(Variant.class.getName());
                }

                if (type.contains("FileDescriptor")) {
                    imports.add(FileDescriptor.class.getName());
                }
                if (type.contains("Map<")) {
                    imports.add(Map.class.getName());
                }
                if (type.contains("Properties")) {
                    imports.add(Properties.class.getName());
                }
                if (type.contains("UInt16")) {
                    imports.add(UInt16.class.getName());
                }
                if (type.contains("UInt32")) {
                    imports.add(UInt32.class.getName());
                }
                if (type.contains("UInt64")) {
                    imports.add(UInt64.class.getName());
                }

                if (type.contains("DBusPath")) {
                    imports.add(DBusPath.class.getName());
                }

                if (type.startsWith("ThreeTuple")) {
                    imports.add(ThreeTuple.class.getName());
                    imports.add(Variant.class.getName());
                    imports.add(Map.class.getName());
                    imports.add(DBusPath.class.getName());

                } else if (type.startsWith("TwoTuple")) {
                    imports.add(TwoTuple.class.getName());
                    imports.add(Variant.class.getName());
                    imports.add(Map.class.getName());
                    imports.add(DBusPath.class.getName());
                }
            }

            // fix imports
            StringBuilder sbImport = new StringBuilder();
            for (String string : imports) {
                sbImport.append("import ").append(string).append(";").append(nl);
            }
            sbImport.append(nl);

            result = result.replace("IMPORTS_PLACEHOLDER", sbImport.toString());
            if (is.packageName == null) {
                System.err.println(is);
                continue;
            }
            File outputPath = new File(_args[1] + File.separator + is.packageName.replace(".", File.separator));
            System.out.println("-> Writing: " + outputPath + File.separator + is.interfaceName + ".java");

            Files.createDirectories(outputPath.toPath());
            FileIoUtil.writeTextFile(outputPath + File.separator + is.interfaceName + ".java", result, false);

        }
    }

    private static void createSignals(StringBuilder _sb, InterfaceStructure _is, String _indent, String _nl) {
        for (InterfaceSignal signal : _is.signals) {
            _sb.append(_indent).append("/**").append(_nl);
            if (signal.documentation.isEmpty()) {
                _sb.append(_indent).append(" * (No documentation available).").append(_nl);
            } else {
                for (String docLine : signal.documentation) {
                    _sb.append(_indent).append(" * ").append(docLine).append("<br>").append(_nl);
                }
            }
            _sb.append(_indent).append(" */").append(_nl);


            _sb.append(_indent).append("public class ").append(signal.signalName).append(" extends DBusSignal {").append(_nl);
            List<String> paramWithType = new ArrayList<>();
            List<String> paramWithoutType = new ArrayList<>();
            for (Map<String, String> fld : signal.fields) {
                Entry<String, String> next = fld.entrySet().iterator().next();
                _sb.append(_indent).append(_indent).append("private ").append(next.getKey()).append(" ").append(next.getValue().replaceFirst("_", "")).append(";").append(_nl);
                paramWithType.add(next.getKey() + " " + next.getValue());
                paramWithoutType.add(next.getValue());
            }
            _sb.append(_nl);
            _sb.append(_indent).append(_indent).append("public ").append(signal.signalName).append("(String _path, ").append(String.join(", ", paramWithType)).append(") throws DBusException {").append(_nl);
            _sb.append(_indent).append(_indent).append(_indent).append("super(_path, ").append(String.join(", ", paramWithoutType)).append(");").append(_nl);
            for (Map<String, String> fld : signal.fields) {
                Entry<String, String> next = fld.entrySet().iterator().next();
                _sb.append(_indent).append(_indent).append(_indent).append("this.").append(next.getValue().replaceFirst("_", "")).append(" = ").append(next.getValue()).append(";").append(_nl);
            }
            _sb.append(_indent).append(_indent).append("}").append(_nl);
            _sb.append(_nl);
            for (Map<String, String> fld : signal.fields) {
                Entry<String, String> next = fld.entrySet().iterator().next();
                _sb.append(_indent).append(_indent).append("public ").append(next.getKey()).append(" ").append("get").append(StringUtil.upperCaseFirstChar(next.getValue().replaceFirst("_", ""))).append("() {").append(_nl);
                _sb.append(_indent).append(_indent).append(_indent).append("return ").append(next.getValue().replaceFirst("_", "")).append(";").append(_nl);
                _sb.append(_indent).append(_indent).append("}").append(_nl);
            }
            _sb.append(_indent).append("}").append(_nl);
        }
    }

    private static int createInterfaceMethod(InterfaceStructure _is, List<String> _list) {
        InterfaceMethod im = null;
        int i;
        String requireNext = null;
        for (i = 0; i < _list.size(); i++) {
            String line = _list.get(i);
            if (requireNext != null) {
                line = requireNext + line;
                requireNext = null;
            }

            if (StringUtil.isBlank(line)) {
                continue;
            }

            if (line.startsWith("=")) {
                return i-1;
            }

            if (line.startsWith("Properties")) {
                _is.additionalInterfaces.add("Properties");
                i += readProperties(_is, _list.subList(i, _list.size()));
                continue;
            } else if (line.startsWith("Signal")) {
                i += readSignals(_is, _list.subList(i, _list.size()));
                continue;
            } else if (!line.startsWith("Methods") && !line.contains(")") && line.matches("^[A-Za-z0-9].+")) {
                return i;
            }

            if (im == null) {
                im = new InterfaceMethod();
            }

            if (im.methodName != null) {
                return i;
            }


            if (line.startsWith("Methods")) {
                line = line.replaceAll("Methods\\s+", "");
            } else if (line.startsWith("\t\t") && !line.startsWith("\t\t\t")) { // methods indented twice, three levels mean comment
                line = line.replaceAll("^\t\t", "");
            }

            if (line.contains("[Deprecated]")) {
                im.deprecated = true;
                line = line.replace("[Deprecated]", "");
            }
                line = line.replaceFirst("^\\s+", "");
                if (StringUtil.isBlank(line)) {
                    continue;
                }

                Matcher matcher = METHOD_REGEX_3PARTS.matcher(line);

                String methodParams = null;
                if (matcher.matches()) {

                    if (StringUtil.isBlank(matcher.group(2))) {
                        im.returnType = "void";
                        im.methodName = matcher.group(1);
                    } else {
                        im.methodName = matcher.group(2);
                        im.returnType = convertDataType(matcher.group(1));
                    }

                    methodParams = matcher.group(3);

                    for (String param : methodParams.split(",")) {
                        im.methodParameters.add(createMethodParam(param.trim()));
                    }
                    i += readCommentAndExceptions(im, _list.subList(i+1, _list.size()));
                    _is.methods.add(im);
                    im = null;
                } else {
                    matcher = METHOD_REGEX_2PARTS.matcher(line);
                    if (matcher.matches()) {
                        im.returnType = "void";
                        im.methodName = matcher.group(1);

                        methodParams = matcher.group(2);

                        for (String param : methodParams.split(",")) {
                            im.methodParameters.add(createMethodParam(param.trim()));
                        }
                        i += readCommentAndExceptions(im, _list.subList(i+1, _list.size()));
                        _is.methods.add(im);
                        im = null;
                    } else { // maybe multiline
                        if (!line.trim().endsWith(")")) {
                            requireNext = line.trim();
                            continue;
                        }
                    }
                }

            }

        return i;
    }



    private static int readSignals(InterfaceStructure _is, List<String> _subList) {
        int i;
        InterfaceSignal signal = null;
        for (i = 0; i < _subList.size(); i++) {
            String line = _subList.get(i);

            if (line.startsWith("=")) {
                return i-1;
            } else if (line.startsWith("Method")) {
                return i-1;
            } else if (line.startsWith("Properties")) {
                return i-1;
            } else if (line.startsWith("Signal")) {
                line = line.replaceAll("Signal.?\\s*", "");
            } else if (line.startsWith("\t\t\t")) {
                line = line.trim();
                if (signal != null) {
                    signal.documentation.add(line);
                }
                continue;
            }

            Matcher matcher = METHOD_REGEX_3PARTS.matcher(line);
            if (matcher.matches()) {
                signal = new InterfaceSignal();
                _is.signals.add(signal);
                signal.signalName = matcher.group(2);

                String methodParams = matcher.group(3);

                for (String param : methodParams.split(",")) {
                    signal.fields.add(createMethodParam(param.trim()));
                }
            }
        }
        return i;
    }

    private static int readProperties(InterfaceStructure _is, List<String> _subList) {
        int i;
        for (i = 0; i < _subList.size(); i++) {
            String line = _subList.get(i);

            if (line.startsWith("=")) { // next interface class
                return i-1;
            } else if (line.startsWith("Method")) { // method block begins
                return i;
            } else if (line.startsWith("Properties")) {
                line = line.replace("Properties", "\t");
            } else if (line.startsWith("=")) { // next structure begins
                return i;
            } else if (StringUtil.isBlank(line)) {
                _is.propertiesDoc.add(line); // preserve empty lines
                continue;
            } else if (!line.startsWith("\t\t")) {
                continue;
            }
            _is.propertiesDoc.add(line);
        }
        return i;
    }

    /**
     * Try to find java exception class for the given error.
     *
     * @param _bluezFqCn
     * @return
     */
    private static String toJavaExceptionClass(String _bluezFqCn) {
        String pkg = "org.bluez.exceptions.";
        String[] split = _bluezFqCn.split("\\.");
        if (split.length < 2) {
            return _bluezFqCn;
        }
        try {
            String clazz = pkg + "Bluez" + split[split.length-1] + "Exception";
            Class.forName(clazz);
            return clazz;
        } catch (ClassNotFoundException _ex) {
            System.err.println("Possible unknown exception class: " + _bluezFqCn);
            return _bluezFqCn;
        }
    }

    private static int readCommentAndExceptions(InterfaceMethod _im, List<String> _subList) {
        boolean readError = false;
        int i;
        for (i = 0; i < _subList.size(); i++) {
            String line = _subList.get(i);
            if (StringUtil.isBlank(line) && !readError) {
                _im.documentation.add(line);
                continue;
            }
            if (line.startsWith("\t\t\t")) { // comment
                line = line.replaceFirst("^\t\t\t", "");

                if (line.toLowerCase().contains("possible errors:")) {
                    readError = true;
                    line = line.replaceFirst("[^:]+\\s*:", "");
                    line = line.trim();
                    if (line.toLowerCase().equals("none")) {
                        continue;
                    }
                    line = line.replaceAll("([^\\s]+).*", "$1");
                    _im.exceptions.add(toJavaExceptionClass(line));
                    continue;
                }
                if (!readError) {
                    _im.documentation.add(line.replaceAll("^\t\t\t", ""));
                } else {
                    line = line.trim();
                    if (!StringUtil.isBlank(line)) {
                        if (line.matches("\\s*\\w+(\\.\\w+)+\\s*")) {
                            line = line.replaceAll("([^\\s]+).*", "$1");
                            _im.exceptions.add(toJavaExceptionClass(line));
                        }
                    }
                }
            } else {
                return i;
            }
        }
        return i;
    }

    private static Map<String, String> createMethodParam(String _string) {
        Map<String,String> m = new HashMap<>();

        if (StringUtil.isBlank(_string)) {
            return m;
        }

        String[] split = _string.split(" ");
        String dataType;
        String varname;
        if (split.length == 2) {
            dataType = split[0];
            varname = "_" + StringUtil.lowerCaseFirstChar(split[1]);
        } else {
            if (_string.trim().equals("void")) {
                dataType = "";
                varname = "";
            } else {
                dataType = "Object";
                varname = _string;
            }

        }

        if (!StringUtils.isBlank(dataType)) {
            dataType = convertDataType(dataType);
        }

        if (!StringUtils.isBlank(varname) && !StringUtils.isBlank(dataType)) {
            m.put(dataType, varname);
        }

        return m;
    }

    private static String convertDataType(String _dataType) {
        _dataType = _dataType.trim();
        if (_dataType.contains("array{")) {
            String replaceAll = _dataType.replaceAll("array\\{(.+)\\}", "$1");
//            if (replaceAll.equals("dict")) {
//                replaceAll = convertDataType("dict");
//            }
            _dataType = convertDataType(replaceAll) + "[]";
        } else if (_dataType.equals("dict")) {
            _dataType = "Map<String, Variant<?>>";
        } else if (_dataType.equals("variant")) {
            _dataType = "Variant<?>";
        } else if (_dataType.equals("void")) {
            _dataType = "void";
        } else if (_dataType.equals("uint16")) {
            _dataType = "UInt16";
        } else if (_dataType.equals("uint32")) {
            _dataType = "UInt32";
        } else if (_dataType.equals("uint64")) {
            _dataType = "UInt64";
        } else if (_dataType.equals("boolean")) {
            _dataType = "boolean";
        } else if (_dataType.equals("byte")) {
            _dataType = "byte";
        } else if (_dataType.equals("int")) {
            _dataType = "int";
        } else if (_dataType.equals("long")) {
            _dataType = "long";
        } else if (_dataType.equals("object")) {
            _dataType = "DBusPath";
        } else if (_dataType.equals("fd")) {
            _dataType = "FileDescriptor";
        } else if (_dataType.contains(",")) { // tuple
            if (_dataType.equals("object, dict")) {
                _dataType = "TwoTuple<DBusPath, Map<String,Variant<?>>>";
            } else {
                String[] split = _dataType.split(",");
                List<String> data = new ArrayList<>();
                for (String string : split) {
                    String[] split2 = string.split(" ");
                    if (split2.length == 2) {
                        data.add(StringUtil.isBlank(split2[0]) ? split2[1] : split2[0]);
                    } else if (split2.length == 1) {
                        data.add(split[0]);
                    }
                }
                if (data.size() == 2) {
                    String type1 = convertDataType(data.get(0));
                    String type2 = convertDataType(data.get(1));

                    _dataType = "TwoTuple<" + type1 + ", " + type2 + ">";
                } else if (data.size() == 3) {
                    String type1 = convertDataType(data.get(0));
                    String type2 = convertDataType(data.get(1));
                    String type3 = convertDataType(data.get(2));

                    _dataType = "ThreeTuple<" + type1 + ", " + type2 + ", " + type3 + ">";
                }
            }
        } else {
            _dataType = StringUtil.upperCaseFirstChar(_dataType);
        }
        return _dataType;
    }

    static class InterfaceStructure {
        private String packageName;
        private String interfaceName;

        private String bluezDocFile;
        private String bluezService;
        private String bluezInterface;

        private Set<String> additionalInterfaces = new LinkedHashSet<>();

        private List<String> bluezObjectPath = new ArrayList<>();

        private List<String> propertiesDoc = new ArrayList<>();

        private List<InterfaceMethod> methods = new ArrayList<>();

        private List<InterfaceSignal> signals = new ArrayList<>();



        public InterfaceStructure() {
            additionalInterfaces.add("DBusInterface"); // all created interfaces will extend the general DBusInterface
        }



        @Override
        public String toString() {
            return getClass().getSimpleName() + " [packageName=" + packageName + ", interfaceName=" + interfaceName
                    + ", methods=" + methods + "]";
        }


    }

    static class InterfaceSignal {
        private String signalName;
        private List<Map<String, String>> fields = new ArrayList<>();
        private List<String> documentation = new ArrayList<>();
    }

    static class InterfaceMethod {
        public boolean deprecated;
        private String returnType;
        private String methodName;
        private List<Map<String, String>> methodParameters = new ArrayList<>();
        private List<String> documentation = new ArrayList<>();
        private List<String> exceptions = new ArrayList<>();
        @Override
        public String toString() {
            return getClass().getSimpleName() + " [deprecated=" + deprecated + ", returnType=" + returnType + ", methodName="
                    + methodName + ", methodParameters=" + methodParameters  // + ", documentation=" + documentation
                    + ", exceptions=" + exceptions + "]";
        }




    }
}
