module org.freedesktop.dbus.transport.junixsocket {
    requires transitive org.newsclub.net.unix;

    requires transitive org.slf4j;

    requires org.freedesktop.dbus;

    provides org.freedesktop.dbus.spi.transport.ITransportProvider
            with org.freedesktop.dbus.transport.junixsocket.JUnixSocketTransportProvider;

    provides org.freedesktop.dbus.spi.message.ISocketProvider
            with org.freedesktop.dbus.transport.junixsocket.JUnixSocketSocketProvider;

    provides org.freedesktop.dbus.spi.filedescriptors.IFileDescriptorHelper
            with org.freedesktop.dbus.transport.junixsocket.JUnixSocketFileDescriptorHelper;
}
