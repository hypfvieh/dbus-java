package org.freedesktop.dbus.utils.bin;

import java.io.IOException;
import java.io.PrintStream;

public abstract class PrintStreamFactory {

    public abstract void init(String _file, String _path);

    /**
     * @param _path path
     * @param _tname target name
     * @return PrintStream
     * @throws IOException may throw exception on failure
     */
    public PrintStream createPrintStream(String _path, String _tname) throws IOException {
        final String file = _path + "/" + _tname + ".java";

        return createPrintStream(file);
    }

    /**
     * @param _file file to print to
     * @return PrintStream
     * @throws IOException may throw exception on failure
     */
    public abstract PrintStream createPrintStream(final String _file) throws IOException;

}