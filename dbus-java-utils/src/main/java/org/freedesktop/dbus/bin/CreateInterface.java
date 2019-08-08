/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.bin;

import static org.freedesktop.dbus.bin.IdentifierMangler.mangle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.types.DBusStructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Converts a DBus XML file into Java interface definitions.
 * @deprecated please use {@link org.freedesktop.dbus.utils.generator.InterfaceCodeGenerator}
 */
@Deprecated
public class CreateInterface {
    private static final Logger logger = LoggerFactory.getLogger(CreateInterface.class);

    @SuppressWarnings("unchecked")
    private static String collapseType(Type t, Set<String> imports, Map<StructStruct, Type[]> structs, boolean container, boolean fullnames) throws DBusException {
        if (t instanceof ParameterizedType) {
            String s;
            Class<? extends Object> c = (Class<? extends Object>) ((ParameterizedType) t).getRawType();
            if (null != structs && t instanceof DBusStructType) {
                int num = 1;
                String name = "Struct";
                while (null != structs.get(new StructStruct(name + num))) {
                    num++;
                }
                name = name + num;
                structs.put(new StructStruct(name), ((ParameterizedType) t).getActualTypeArguments());
                return name;
            }
            if (null != imports) {
                imports.add(c.getName());
            }
            if (fullnames) {
                return c.getName();
            } else {
                s = c.getSimpleName();
            }
            s += '<';
            Type[] ts = ((ParameterizedType) t).getActualTypeArguments();
            for (Type st : ts) {
                s += collapseType(st, imports, structs, true, fullnames) + ',';
            }
            s = s.replaceAll(",$", ">");
            return s;
        } else if (t instanceof Class) {
            Class<? extends Object> c = (Class<? extends Object>) t;
            if (c.isArray()) {
                return collapseType(c.getComponentType(), imports, structs, container, fullnames) + "[]";
            } else {
                Package p = c.getPackage();
                if (null != imports && !"java.lang".equals(p.getName())) {
                    imports.add(c.getName());
                }
                if (container) {
                    if (fullnames) {
                        return c.getName();
                    } else {
                        return c.getSimpleName();
                    }
                } else {
                    try {
                        Field f = c.getField("TYPE");
                        Class<? extends Object> d = (Class<? extends Object>) f.get(c);
                        return d.getSimpleName();
                    } catch (Exception e) {
                        return c.getSimpleName();
                    }
                }
            }
        } else {
            return "";
        }
    }

    private static String getJavaType(String dbus, Set<String> imports, Map<StructStruct, Type[]> structs, boolean container, boolean fullnames) throws DBusException {
        if (null == dbus || "".equals(dbus)) {
            return "";
        }
        List<Type> v = new ArrayList<>();
        Marshalling.getJavaType(dbus, v, 1);
        Type t = v.get(0);
        return collapseType(t, imports, structs, container, fullnames);
    }

    // CHECKSTYLE:OFF
    public String comment = "";
    boolean       builtin;
    private HashMap<String, List<StructStruct>> structPackages = new HashMap<>();
    // CHECKSTYLE:ON

    public CreateInterface(PrintStreamFactory _factory, boolean _builtin) {
        this.factory = _factory;
        this.builtin = _builtin;
    }

    String parseReturns(List<Element> out, Set<String> imports, Map<String, Integer> tuples, Map<StructStruct, Type[]> structs, String pack) throws DBusException {
        logger.debug("parseReturns");

        String[] names = new String[] {
                "Pair", "Triplet", "Quad", "Quintuple", "Sextuple", "Septuple"
        };
        String sig = "";
        String name = null;
        switch (out.size()) {
        case 0:
            sig += "void ";
            break;
        case 1:
            sig += getJavaType(out.get(0).getAttribute("type"), imports, structs, false, false) + " ";
            break;
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
            name = names[out.size() - 2];
        default:
            if (null == name) {
                name = "NTuple" + out.size();
            }

            tuples.put(name, out.size());
            imports.add(pack + "." + name);
            sig += name + "<";
            for (Element arg : out) {
                sig += getJavaType(arg.getAttribute("type"), imports, structs, true, false) + ", ";
            }
            sig = sig.replaceAll(", $", "> ");
            break;
        }
        return sig;
    }

    String parseMethod(Element meth, Set<String> imports, Map<String, Integer> tuples, Map<StructStruct, Type[]> structs, Set<String> exceptions, Set<String> anns, String pack) throws DBusException {
        List<Element> in = new ArrayList<>();
        List<Element> out = new ArrayList<>();
        if (null == meth.getAttribute("name") || "".equals(meth.getAttribute("name"))) {
            System.err.println("ERROR: Method name was blank, failed");
            System.exit(1);
        }
        String annotations = "";
        String throwses = null;

        logger.debug("parseMethod '{}'", meth.getAttribute("name"));

        for (Node a : new IterableNodeList(meth.getChildNodes())) {

            if (Node.ELEMENT_NODE != a.getNodeType()) {
                continue;
            }

            checkNode(a, "arg", "annotation");

            if ("arg".equals(a.getNodeName())) {
                Element arg = (Element) a;

                // methods default to in
                if ("out".equals(arg.getAttribute("direction"))) {
                    out.add(arg);
                } else {
                    in.add(arg);
                }
            } else if ("annotation".equals(a.getNodeName())) {
                Element e = (Element) a;
                if (e.getAttribute("name").equals("org.freedesktop.DBus.Method.Error")) {
                    if (null == throwses) {
                        throwses = e.getAttribute("value");
                    } else {
                        throwses += ", " + e.getAttribute("value");
                    }
                    exceptions.add(e.getAttribute("value"));
                } else {
                    annotations += parseAnnotation(e, imports, anns);
                }
            }
        }

        String sig = "";
        comment = "";
        sig += parseReturns(out, imports, tuples, structs, pack);

        sig += mangle(meth.getAttribute("name")) + "(";

        char defaultname = 'a';
        String params = "";
        for (Element arg : in) {
            String type = getJavaType(arg.getAttribute("type"), imports, structs, false, false);
            String name = arg.getAttribute("name");
            if (null == name || "".equals(name)) {
                name = "" + (defaultname++);
            }
            params += type + " " + mangle(name) + ", ";
        }
        return ("".equals(comment) ? "" : "   /**\n" + comment + "   */\n") + annotations + "  public " + sig + params.replaceAll("..$", "") + ")" + (null == throwses ? "" : " throws " + throwses) + ";";
    }

    String parseSignal(Element signal, Set<String> imports, Map<StructStruct, Type[]> structs, Set<String> anns) throws DBusException {
        logger.debug("parseSignal");

        Map<String, String> params = new HashMap<String, String>();
        List<String> porder = new ArrayList<>();
        char defaultname = 'a';
        imports.add("org.freedesktop.dbus.messages.DBusSignal");
        imports.add("org.freedesktop.dbus.exceptions.DBusException");
        String annotations = "";
        for (Node a : new IterableNodeList(signal.getChildNodes())) {

            if (Node.ELEMENT_NODE != a.getNodeType()) {
                continue;
            }

            checkNode(a, "arg", "annotation");

            if ("annotation".equals(a.getNodeName())) {
                annotations += parseAnnotation((Element) a, imports, anns);
            } else {
                Element arg = (Element) a;
                String type = getJavaType(arg.getAttribute("type"), imports, structs, false, false);
                String name = arg.getAttribute("name");
                if (null == name || "".equals(name)) {
                    name = "" + (defaultname++);
                }
                params.put(mangle(name), type);
                porder.add(mangle(name));
            }
        }

        String out = "";
        out += annotations;
        out += "   public static class " + signal.getAttribute("name");
        out += " extends DBusSignal\n   {\n";
        for (String name : porder) {
            out += "      public final " + params.get(name) + " " + name + ";\n";
        }
        out += "      public " + signal.getAttribute("name") + "(String path";
        for (String name : porder) {
            out += ", " + params.get(name) + " " + name;
        }
        out += ") throws DBusException\n      {\n         super(path";
        for (String name : porder) {
            out += ", " + name;
        }
        out += ");\n";
        for (String name : porder) {
            out += "         this." + name + " = " + name + ";\n";
        }
        out += "      }\n";

        out += "   }\n";
        return out;
    }

    String parseAnnotation(Element ann, Set<String> imports, Set<String> annotations) {
        logger.debug("parseAnnotation");

        String s = "  @" + ann.getAttribute("name").replaceAll(".*\\.([^.]*)$", "$1") + "(";
        if (null != ann.getAttribute("value") && !"".equals(ann.getAttribute("value"))) {
            s += '"' + ann.getAttribute("value") + '"';
        }
        imports.add(ann.getAttribute("name"));
        annotations.add(ann.getAttribute("name"));
        return s += ")\n";
    }

    public class InterfaceDefinition {
        String interfaceName;
        String packageName;
        String className;

        String file;
        String path;

        Set<String> imports = new TreeSet<String>();
        String methods = "";
        String signals = "";
        String annotations;

        void write(PrintStream out) {
            out.println("package " + packageName + ";");
            out.println();

            if (imports.size() > 0) {
                for (String i : imports) {
                    out.println("import " + i + ";");
                }
            }

            out.println(annotations);
            out.print("public interface " + className);
            out.println(" extends DBusInterface");
            out.println("{");
            out.println(signals);
            out.println(methods);
            out.println("}");
        }
    }

    InterfaceDefinition parseInterface(Element iface, Map<String, Integer> tuples, Map<StructStruct, Type[]> structs, Set<String> exceptions, Set<String> anns, String pack) throws DBusException {

        if (null == iface.getAttribute("name") || "".equals(iface.getAttribute("name"))) {
            System.err.println("ERROR: Interface name was blank, failed");
            System.exit(1);
        }

        logger.info("  - create interface '{}'", iface.getAttribute("name"));

        InterfaceDefinition def = new InterfaceDefinition();

        def.interfaceName   = iface.getAttribute("name");
        def.packageName     = def.interfaceName.replaceAll("\\.[^.]*$", "");

        String parentPack = def.interfaceName.replaceAll("\\.[^.]*$", "");
        String interfaceName = def.interfaceName.replaceAll("^.*\\.([^.]*)$", "$1");
        def.className = "I" + interfaceName.substring(0, 1).toUpperCase() + interfaceName.substring(1);

        def.file = parentPack.replaceAll("\\.", "/") + "/" + def.className + ".java";
        def.path = def.file.replaceAll("/[^/]*$", "");

        def.annotations     = String.format("\n@DBusInterfaceName(value = \"%s\")", def.interfaceName);

        def.imports.add("org.freedesktop.dbus.interfaces.DBusInterface");
        def.imports.add("org.freedesktop.dbus.annotations.DBusInterfaceName");

        for (Node meth : new IterableNodeList(iface.getChildNodes())) {

            if (Node.ELEMENT_NODE != meth.getNodeType()) {
                continue;
            }

            checkNode(meth, "method", "signal", "property", "annotation");

            if ("method".equals(meth.getNodeName())) {
                def.methods += parseMethod((Element) meth, def.imports, tuples, structs, exceptions, anns, pack) + "\n";
            } else if ("signal".equals(meth.getNodeName())) {
                def.signals += parseSignal((Element) meth, def.imports, structs, anns);
            } else if ("property".equals(meth.getNodeName())) {
                logger.debug("WARNING: Ignoring property");
            } else if ("annotation".equals(meth.getNodeName())) {
                def.annotations += parseAnnotation((Element) meth, def.imports, anns);
            }
        }

        return def;
    }


    void createException(String name, String pack, PrintStream out) throws DBusException {
        logger.info("  - create exception '{}'", name);

        out.println("package " + pack + ";");
        out.println("import org.freedesktop.dbus.exceptions.DBusExecutionException;");
        out.print("public class " + name);
        out.println(" extends DBusExecutionException");
        out.println("{");
        out.println("   public " + name + "(String message)");
        out.println("   {");
        out.println("      super(message);");
        out.println("   }");
        out.println("}");
    }

    void createAnnotation(String name, String pack, PrintStream out) throws DBusException {
        logger.info("  - create annotation '{}'", name);

        out.println("package " + pack + ";");
        out.println("import java.lang.annotation.Retention;");
        out.println("import java.lang.annotation.RetentionPolicy;");
        out.println("@Retention(RetentionPolicy.RUNTIME)");
        out.println("public @interface " + name);
        out.println("{");
        out.println("   String value();");
        out.println("}");
    }

    void createStruct(String name, Type[] type, String pack, PrintStream out, Map<StructStruct, Type[]> existing) throws DBusException, IOException {
        logger.info("  - create struct '{}.{}'", pack, name);

        out.println("package " + pack + ";");

        Set<String> imports = new TreeSet<String>();
        imports.add("org.freedesktop.dbus.annotations.Position");
        imports.add("org.freedesktop.dbus.Struct");
        imports.add(pack + ".*");
        Map<StructStruct, Type[]> structs = new HashMap<StructStruct, Type[]>(existing);
        String[] types = new String[type.length];
        for (int i = 0; i < type.length; i++) {
            types[i] = collapseType(type[i], imports, structs, false, false);
        }

        for (String im : imports) {
            out.println("import " + im + ";");
        }

        out.println("public final class " + name + " extends Struct");
        out.println("{");
        int i = 0;
        char c = 'a';
        String params = "";
        for (String t : types) {
            out.println("   @Position(" + i++ + ")");
            out.println("   public final " + t + " " + c + ";");
            params += t + " " + c + ", ";
            c++;
        }
        out.println("  public " + name + "(" + params.replaceAll("..$", "") + ")");
        out.println("  {");
        for (char d = 'a'; d < c; d++) {
            out.println("   this." + d + " = " + d + ";");
        }

        out.println("  }");
        out.println("}");

        structs = StructStruct.fillPackages(structs, pack);
        Map<StructStruct, Type[]> tocreate = new HashMap<StructStruct, Type[]>(structs);
        for (StructStruct ss : existing.keySet()) {
            tocreate.remove(ss);
        }

        createStructs(tocreate, structs);
    }

    void createTuple(String name, int num, String pack, PrintStream out) throws DBusException {
        logger.info("  - create tuple '{}'", name);

        out.println("package " + pack + ";");
        out.println("import org.freedesktop.dbus.annotations.Position;");
        out.println("import org.freedesktop.dbus.Tuple;");
        out.println("/** Just a typed container class */");
        out.print("public final class " + name);
        String types = " <";
        for (char v = 'A'; v < 'A' + num; v++) {
            types += v + ",";
        }
        out.print(types.replaceAll(",$", "> "));
        out.println("extends Tuple");
        out.println("{");

        char t = 'A';
        char n = 'a';
        for (int i = 0; i < num; i++, t++, n++) {
            out.println("   @Position(" + i + ")");
            out.println("   public final " + t + " " + n + ";");
        }

        out.print("   public " + name + "(");
        String sig = "";
        t = 'A';
        n = 'a';
        for (int i = 0; i < num; i++, t++, n++) {
            sig += t + " " + n + ", ";
        }
        out.println(sig.replaceAll(", $", ")"));
        out.println("   {");
        for (char v = 'a'; v < 'a' + num; v++) {
            out.println("      this." + v + " = " + v + ";");
        }
        out.println("   }");

        out.println("}");
    }

    void parseRoot(Element root) throws DBusException, IOException {

        ArrayList<InterfaceDefinition> interfaceDefs = new ArrayList<>();

        Map<StructStruct, Type[]> structs = new HashMap<StructStruct, Type[]>();
        Set<String> exceptions = new TreeSet<String>();
        Set<String> annotations = new TreeSet<String>();

        for (Node iface : new IterableNodeList(root.getChildNodes())) {

            if (Node.ELEMENT_NODE != iface.getNodeType()) {
                continue;
            }

            checkNode(iface, "interface", "node");

            if ("interface".equals(iface.getNodeName())) {

                Map<String, Integer> tuples = new HashMap<String, Integer>();
                String name = ((Element) iface).getAttribute("name");
                String pack = name;

                // don't create interfaces in org.freedesktop.DBus by default
                if (pack.startsWith("org.freedesktop.DBus") && !builtin) {
                    continue;
                }

                InterfaceDefinition def = parseInterface((Element) iface, tuples, structs, exceptions, annotations, pack);
                interfaceDefs.add(def);

                structs = StructStruct.fillPackages(structs, pack);

                createTuples(tuples, pack);
            } else if ("node".equals(iface.getNodeName())) {
                parseRoot((Element) iface);
            } else {
                System.err.println("ERROR: Unknown node: " + iface.getNodeName());
                System.exit(1);
            }
        }

        createStructs(structs, structs);
        createExceptions(exceptions);
        createAnnotations(annotations);

        for (InterfaceDefinition def : interfaceDefs) {
            for (String sspack : structPackages.keySet()) {
                def.imports.add(sspack + ".*");
            }

            logger.info ( "  - writing interface {}.{}", def.packageName, def.className);
            factory.init(def.file, def.path);
            def.write (factory.createPrintStream(def.file));
        }
    }

    private void createAnnotations(Set<String> annotations) throws DBusException, IOException {
        logger.debug("createAnnotations");

        for (String fqn : annotations) {
            String name = fqn.replaceAll("^.*\\.([^.]*)$", "$1");
            String pack = fqn.replaceAll("\\.[^.]*$", "");
            // don't create things in org.freedesktop.DBus by default
            if (pack.startsWith("org.freedesktop.DBus") && !builtin) {
                continue;
            }
            String path = pack.replaceAll("\\.", "/");
            String file = name.replaceAll("\\.", "/") + ".java";
            factory.init(file, path);
            createAnnotation(name, pack, factory.createPrintStream(path, name));
        }
    }

    private void createExceptions(Set<String> exceptions) throws DBusException, IOException {
        logger.debug("createExceptions");

        for (String fqn : exceptions) {
            String name = fqn.replaceAll("^.*\\.([^.]*)$", "$1");
            String pack = fqn.replaceAll("\\.[^.]*$", "");
            // don't create things in org.freedesktop.DBus by default
            if (pack.startsWith("org.freedesktop.DBus") && !builtin) {
                continue;
            }
            String path = pack.replaceAll("\\.", "/");
            String file = name.replaceAll("\\.", "/") + ".java";
            factory.init(file, path);
            createException(name, pack, factory.createPrintStream(path, name));
        }
    }

    private void createStructs(Map<StructStruct, Type[]> structs, Map<StructStruct, Type[]> existing) throws DBusException, IOException {
        logger.debug("createStructs");

        for (StructStruct ss : structs.keySet()) {
            String file = ss.name.replaceAll("\\.", "/") + ".java";
            String path = ss.pack.replaceAll("\\.", "/");
            factory.init(file, path);
            createStruct(ss.name, structs.get(ss), ss.pack, factory.createPrintStream(path, ss.name), existing);

            List<StructStruct> packageStructs;
            if (structPackages.containsKey(ss.pack)) {
                packageStructs = structPackages.get(ss.pack);
            } else {
                packageStructs = new ArrayList<>();
                structPackages.put(ss.pack, packageStructs);
            }
            packageStructs.add(ss);
        }
    }

    private void createTuples(Map<String, Integer> typeMap, String pack) throws DBusException, IOException {
        logger.debug("createTuples");

        for (String tname : typeMap.keySet()) {
            createTuple(tname, typeMap.get(tname), pack, factory.createPrintStream(pack.replaceAll("\\.", "/"), tname));
        }
    }

    static class ConsoleStreamFactory extends PrintStreamFactory {

        @Override
        public void init(String file, String path) {
        }

        @Override
        public PrintStream createPrintStream(String file) throws IOException {
            logger.debug("Writing to {}", file);
            System.out.println("/* File: " + file + " */");
            return System.out;
        }

        @Override
        public PrintStream createPrintStream(String path, String tname) throws IOException {
            return super.createPrintStream(path, tname);
        }

    }

    static class FileStreamFactory extends PrintStreamFactory {
        @Override
        public void init(String file, String path) {
            new File(path).mkdirs();
        }

        /**
         * @param file
         * @return
         * @throws IOException
         */
        @Override
        public PrintStream createPrintStream(final String file) throws IOException {
            logger.debug("Writing to {}", file);
            return new PrintStream(new FileOutputStream(file));
        }

    }

    static void checkNode(Node n, String... names) {
        String expected = "";
        for (String name : names) {
            if (name.equals(n.getNodeName())) {
                return;
            }
            expected += name + " or ";
        }
        System.err.println(String.format("ERROR: Expected %s, got %s, failed.", expected.replaceAll("....$", ""), n.getNodeName()));
        System.exit(1);
    }

    private final PrintStreamFactory factory;

    static class Config {
        // CHECKSTYLE:OFF
        DBusBusType     bus       = DBusBusType.SESSION;
        String  busname   = null;
        String  object    = null;
        File    datafile  = null;
        boolean printtree = false;
        boolean fileout   = false;
        boolean builtin   = false;
        boolean ignoreDtd = true;
        // CHECKSTYLE:ON
    }

    static void printSyntax() {
        printSyntax(System.err);
    }

    static void printSyntax(PrintStream o) {
        o.println("Syntax: CreateInterface <options> [file | busname object]");
        o.println("        Options: --no-ignore-builtin  --enable-dtd-validation --system -y --session -s --create-files -f --help -h --version -v");
    }

    public static void version() {
        System.out.println("Java D-Bus Version " + System.getProperty("Version"));
        System.exit(1);
    }

    static Config parseParams(String[] args) {
        Config config = new Config();
        for (String p : args) {
            if ("--system".equals(p) || "-y".equals(p)) {
                config.bus = DBusBusType.SYSTEM;
            } else if ("--session".equals(p) || "-s".equals(p)) {
                config.bus = DBusBusType.SESSION;
            } else if ("--no-ignore-builtin".equals(p)) {
                config.builtin = true;
            } else if ("--create-files".equals(p) || "-f".equals(p)) {
                config.fileout = true;
            } else if ("--print-tree".equals(p) || "-p".equals(p)) {
                config.printtree = true;
            } else if ("--enable-dtd-validation".equals(p)) {
                config.ignoreDtd = false;
            } else if ("--help".equals(p) || "-h".equals(p)) {
                printSyntax(System.out);
                System.exit(0);
            } else if ("--version".equals(p) || "-v".equals(p)) {
                version();
                System.exit(0);
            } else if (p.startsWith("-")) {
                System.err.println("ERROR: Unknown option: " + p);
                printSyntax();
                System.exit(1);
            } else {
                if (null == config.busname) {
                    config.busname = p;
                } else if (null == config.object) {
                    config.object = p;
                } else {
                    printSyntax();
                    System.exit(1);
                }
            }
        }
        if (null == config.busname) {
            printSyntax();
            System.exit(1);
        } else if (null == config.object) {
            config.datafile = new File(config.busname);
            config.busname = null;
        }
        return config;
    }

    public static void main(String[] args) throws Exception {
        Config config = parseParams(args);

        Reader introspectdata = null;

        if (null != config.busname) {
            try {

                logger.info("Introspecting:");
                logger.info("  - interface '{}'", config.busname);
                logger.info("  - objectPath '{}'", config.object);

                DBusConnection conn = DBusConnection.getConnection(config.bus);
                Introspectable in = conn.getRemoteObject(config.busname, config.object, Introspectable.class);
                String id = in.Introspect();
                if (null == id) {
                    System.err.println("ERROR: Failed to get introspection data");
                    System.exit(1);
                }
                introspectdata = new StringReader(id);
                conn.disconnect();
            } catch (DBusException exD) {
                System.err.println("ERROR: Failure in DBus Communications: " + exD.getMessage());
                System.exit(1);
            } catch (DBusExecutionException exDee) {
                System.err.println("ERROR: Failure in DBus Communications: " + exDee.getMessage());
                System.exit(1);

            }
        } else if (null != config.datafile) {
            try {
                introspectdata = new InputStreamReader(new FileInputStream(config.datafile));
            } catch (FileNotFoundException exFnf) {
                System.err.println("ERROR: Could not find introspection file: " + exFnf.getMessage());
                System.exit(1);
            }
        }
        try {
            logger.info("Generating code: ");

            PrintStreamFactory factory = config.fileout ? new FileStreamFactory() : new ConsoleStreamFactory();
            CreateInterface createInterface = new CreateInterface(factory, config.builtin);
            createInterface.createInterface(introspectdata, config);
        } catch (DBusException exD) {
            System.err.println("ERROR: " + exD.getMessage());
            System.exit(1);
        }
    }

    /** Output the interface for the supplied xml reader
    * @param _introspectdata The introspect data reader
    * @param _config config
    * @throws ParserConfigurationException If the xml parser could not be configured
    * @throws SAXException If a problem occurs reading the xml data
    * @throws IOException If an IO error occurs
    * @throws DBusException If the dbus related error occurs
    */
    public void createInterface(Reader _introspectdata, Config _config) throws ParserConfigurationException, SAXException, IOException, DBusException {
        DocumentBuilderFactory lfactory = DocumentBuilderFactory.newInstance();

        if (_config != null && _config.ignoreDtd) { // if dtd validation is disabled (default)
            lfactory.setValidating(false);
            lfactory.setNamespaceAware(true);
            lfactory.setFeature("http://xml.org/sax/features/namespaces", false);
            lfactory.setFeature("http://xml.org/sax/features/validation", false);
            lfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            lfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }

        DocumentBuilder builder = lfactory.newDocumentBuilder();

        Document document = builder.parse(new InputSource(_introspectdata));

        Element root = document.getDocumentElement();
        checkNode(root, "node");
        parseRoot(root);

    }
}
