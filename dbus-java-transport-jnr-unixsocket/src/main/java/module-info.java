module org.freedesktop.dbus.transport.jnr {
    requires transitive org.jnrproject.constants;
    requires transitive org.jnrproject.enxio;
    requires transitive org.jnrproject.ffi;
    requires transitive org.jnrproject.posix;
    requires transitive org.jnrproject.unixsocket;

    requires transitive org.slf4j;

    requires org.freedesktop.dbus;

    provides org.freedesktop.dbus.spi.transport.ITransportProvider
            with
            org.freedesktop.dbus.transport.jnr.JnrUnixSocketTransportProvider;

}
