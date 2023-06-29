module org.freedesktop.dbus.transport.jre {
    requires jdk.net;

    requires org.freedesktop.dbus;

    provides org.freedesktop.dbus.spi.transport.ITransportProvider
            with
            org.freedesktop.dbus.transport.jre.NativeTransportProvider;
}