module org.freedesktop.dbus.transport.tcp {

    requires org.freedesktop.dbus;

    provides org.freedesktop.dbus.spi.transport.ITransportProvider
            with
            org.freedesktop.dbus.transport.tcp.TcpTransportProvider;
}