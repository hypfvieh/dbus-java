package org.freedesktop.dbus.utils.generator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * The {@code GeneratedCodeCompiler} class is a utility for compiling generated Java source code in memory.
 * It verifies that generated code is valid and free of compilation errors in the test environment, ensuring
 * compatibility with required dependencies and correct formatting. Neither the sources nor the compiled
 * classes are written to disk.
 */
public final class GeneratedCodeCompiler {

    private GeneratedCodeCompiler() {

    }

    /**
     * Compiles all generated sources together fully in memory against the current test classpath (which contains
     * the dbus-java-core annotations/types). Fails with the collected compiler errors if the generated code does
     * not compile. This catches import/formatting/generics regressions that the plain string assertions cannot.
     */
    static void assertCompiles(String _desc, Map<File, String> _generated) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK (with javac) is required to run this test");
        assertFalse(_generated.isEmpty(), _desc + " - no sources were generated");

        List<JavaFileObject> sources = new ArrayList<>();
        for (Entry<File, String> e : _generated.entrySet()) {
            String rel = e.getKey().getPath().replace(File.separatorChar, '/');
            String fqcn = rel.substring(0, rel.length() - JavaFileObject.Kind.SOURCE.extension.length()).replace('/', '.');
            sources.add(new InMemorySource(fqcn, e.getValue()));
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        // dependencies (dbus-java-core) may be on the module path in a modular surefire run, so combine both
        String classpath = Stream.of(System.getProperty("java.class.path"), System.getProperty("jdk.module.path"))
            .filter(p -> p != null && !p.isBlank())
            .collect(Collectors.joining(File.pathSeparator));
        List<String> options = List.of("-classpath", classpath, "-proc:none");

        try (StandardJavaFileManager stdManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
                InMemoryFileManager fileManager = new InMemoryFileManager(stdManager)) {

            boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, sources).call();

            String errors = diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .map(d -> (d.getSource() == null ? "" : d.getSource().getName() + ":" + d.getLineNumber() + ": ") + d.getMessage(null))
                .collect(Collectors.joining("\n"));

            assertTrue(ok, _desc + " - generated code did not compile:\n" + errors);
        }
    }

    /** In-memory Java source for the compiler. */
    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        InMemorySource(String _fqcn, String _code) {
            super(URI.create("string:///" + _fqcn.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            code = _code;
        }

        @Override
        public CharSequence getCharContent(boolean _ignoreEncodingErrors) {
            return code;
        }
    }

    /** In-memory compiled class; holds the bytecode in a buffer instead of writing it to disk. */
    private static final class InMemoryClass extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        InMemoryClass(String _className, Kind _kind) {
            super(URI.create("mem:///" + _className.replace('.', '/') + _kind.extension), _kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }

    /** File manager which redirects compiled class output into {@link InMemoryClass} buffers (no disk output). */
    private static final class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        InMemoryFileManager(StandardJavaFileManager _delegate) {
            super(_delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location _location, String _className,
                JavaFileObject.Kind _kind, FileObject _sibling) {
            return new InMemoryClass(_className, _kind);
        }
    }
}
