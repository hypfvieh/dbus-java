package org.freedesktop.dbus.spi.message;

import org.freedesktop.dbus.FileDescriptor;

import java.nio.channels.SocketChannel;
import java.util.List;

public class OutputStreamMessageWriter extends AbstractOutputStreamMessageWriter {

    public OutputStreamMessageWriter(SocketChannel _out) {
        super(_out, DefaultSocketProvider.INSTANCE);
    }

    @Override
    protected void writeFileDescriptors(SocketChannel _outputChannel, List<FileDescriptor> _filedescriptors) {
        // not supported
    }

}
