package org.freedesktop.dbus.transport.junixsocket;

import org.freedesktop.dbus.spi.message.AbstractOutputStreamMessageWriter;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import org.newsclub.net.unix.FileDescriptorCast;

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
                FileDescriptor[] fds = new FileDescriptor[_filedescriptors.size()];
                for (int i = 0; i < _filedescriptors.size(); i++) {
                    fds[i] = FileDescriptorCast.unsafeUsing(_filedescriptors.get(i).getIntFileDescriptor()).getFileDescriptor();
                }
                ((AFUNIXSocketChannel) _outputChannel).setOutboundFileDescriptors(fds);
            } else {
                ((AFUNIXSocketChannel) _outputChannel).setOutboundFileDescriptors((FileDescriptor[]) null);
            }
        }
    }

}
