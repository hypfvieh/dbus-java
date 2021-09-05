module org.freedesktop.dbus.transport.jnr {
    requires jdk.security.auth;
    requires jdk.net;

    requires org.freedesktop.dbus;

    //provides org.freedesktop.dbus.transport.jre.NativeTransportProvider with org.freedesktop.dbus.spi.transport.ITransportProvider;
}