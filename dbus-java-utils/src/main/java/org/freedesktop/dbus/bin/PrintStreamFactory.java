package org.freedesktop.dbus.bin;

import java.io.IOException;
import java.io.PrintStream;

public abstract class PrintStreamFactory {

    public abstract void init(String file, String path);

    /**
     * @param path path
     * @param tname target name
     * @return PrintStream
     * @throws IOException may throw exception on failure
     */
    public PrintStream createPrintStream(String path, String tname) throws IOException {
        final String file = path + "/" + tname + ".java";

        return createPrintStream(file);
    }

    /**
     * @param file file to print to
     * @return PrintStream
     * @throws IOException may throw exception on failure
     */
    public abstract PrintStream createPrintStream(final String file) throws IOException;

}