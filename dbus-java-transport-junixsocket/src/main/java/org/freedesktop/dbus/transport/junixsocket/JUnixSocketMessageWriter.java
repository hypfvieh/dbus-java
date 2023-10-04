package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.exceptions.MarshallingException;
import org.freedesktop.dbus.spi.message.AbstractOutputStreamMessageWriter;
import org.newsclub.net.unix.AFUNIXSocketChannel;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

public class JUnixSocketMessageWriter extends AbstractOutputStreamMessageWriter {

    public JUnixSocketMessageWriter(AFUNIXSocketChannel _socket, boolean _hasFileDescriptorSupport) {
        super(_socket, _hasFileDescriptorSupport);
    }

    @Override
    protected void writeFileDescriptors(SocketChannel _outputChannel, List<org.freedesktop.dbus.FileDescriptor> _filedescriptors) throws IOException {
        if (_outputChannel instanceof AFUNIXSocketChannel) {
            if (_filedescriptors != null && !_filedescriptors.isEmpty()) {
                try {
                    FileDescriptor[] fds = new FileDescriptor[_filedescriptors.size()];
                    for (int i = 0; i < _filedescriptors.size(); i++) {
                        fds[i] = _filedescriptors.get(i).toJavaFileDescriptor();
                    }
                    ((AFUNIXSocketChannel) _outputChannel).setOutboundFileDescriptors(fds);
                } catch (MarshallingException _ex) {
                    throw new IOException("unable to marshall file descriptors", _ex);
                }
            } else {
                ((AFUNIXSocketChannel) _outputChannel).setOutboundFileDescriptors((FileDescriptor[]) null);
            }
        }
    }

}
